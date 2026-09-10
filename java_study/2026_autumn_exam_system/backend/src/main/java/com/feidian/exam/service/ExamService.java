/* 文件导读：考试事务核心：Spring 的代理管理事务，MyBatis 使用事务连接执行 SQL，MySQL InnoDB 的行锁保护并发。默认单例只保存依赖，不保存某个学生的临时答卷状态。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.service;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.BusinessException;
import com.feidian.exam.dto.request.SubmitAttemptRequest;
import com.feidian.exam.dto.response.*;
import com.feidian.exam.mapper.*;
import com.feidian.exam.model.*;
import com.feidian.exam.security.SessionUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考试核心业务。Controller 只负责 HTTP，本类决定能否开考、如何生成试卷及如何交卷。
 * 两个写流程都放在事务中：开考要一起写答卷头和所有题目快照，交卷要一起写明细和总分。
 */
@Service
public class ExamService {
    /** 课程与选课关系 Mapper 代理。 */
    private final CourseMapper courses;
    /** QuestionMapper 代理：开考时读取课程有效题目，再由本类复制为答卷快照。 */
    private final QuestionMapper questions;
    /** ExamMapper 代理：查询/锁定答卷，并在当前事务中保存卷头、快照和判分明细。 */
    private final ExamMapper exams;
    /** 课程角色与资源归属检查组件。 */
    private final CourseAccess access;
    /** 只计算、不访问数据库的判分组件。 */
    private final GradingService grading;

    /** 注入课程、题库、答卷 Mapper 以及权限和判分组件；职责分离让业务流程与数据库实现可单独追踪。 */
    public ExamService(CourseMapper courses, QuestionMapper questions, ExamMapper exams,
                       CourseAccess access, GradingService grading) {
        this.courses = courses;
        this.questions = questions;
        this.exams = exams;
        this.access = access;
        this.grading = grading;
    }

    // Spring 通过代理在公开方法进入前开启事务，正常返回时提交，异常离开时回滚。
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    /** 事务性开考：身份/选课 -> 锁选课行 -> 复用进行中卷或验证 FIRST/RETAKE -> 读取题目 -> 保存卷头和全部快照。 */
    public StartAttemptResponse start(SessionUser student, Long courseId, String mode) {
        access.requireRole(student, "STUDENT");
        if (!"FIRST".equals(mode) && !"RETAKE".equals(mode)) {
            throw BusinessException.badRequest("mode 须为 FIRST 或 RETAKE");
        }
        Course course = access.studentCourse(student, courseId);
        // 对稳定存在的选课行加锁，让同一学生同一课程的并发开考串行检查。
        // READ_COMMITTED 确保等待锁后能看见前一个事务创建的答卷。
        if (courses.lockEnrollment(student.id, courseId) == null) {
            throw BusinessException.forbidden();
        }
        // 所有开考请求先锁同一条 enrollment，再检查状态，才能避免“同时都查不到再各建一张”。
        Attempt active = exams.active(student.id, courseId);
        if (active != null) {
            // 幂等设计：网络重试时返回原答卷，不增加 attempt_no。
            return new StartAttemptResponse(active, true);
        }
        int previousNo = exams.lastAttemptNo(student.id, courseId);
        // 模式是业务状态机：有历史不能再 FIRST，无历史不能直接 RETAKE；进行中卷在上面优先复用。
        if (previousNo > 0 && "FIRST".equals(mode)) {
            throw BusinessException.conflict("RETAKE_REQUIRED", "已完成该课程考试，请通过重考开始");
        }
        if (previousNo == 0 && "RETAKE".equals(mode)) {
            throw BusinessException.conflict("NO_PREVIOUS_ATTEMPT", "尚未参加该课程考试");
        }
        List<Question> current = questions.forExam(courseId);
        // XML 最多取 1001 条；这里明确拒绝空卷和超大卷，尚未写任何答卷数据。
        if (current.isEmpty()) {
            throw BusinessException.conflict("NO_QUESTIONS", "该课程暂无题目");
        }
        if (current.size() > 1000) {
            throw BusinessException.conflict("EXAM_TOO_LARGE", "单张试卷最多支持 1000 道题");
        }
        // 这是学过的 Stream：取每题分值后求和；最终成绩仍由服务端按快照计算。
        int totalScore = current.stream().mapToInt(Question::getPoints).sum();
        Attempt attempt = new Attempt();
        attempt.setStudentId(student.id);
        attempt.setCourseId(courseId);
        attempt.setCourseName(course.getName());
        attempt.setAttemptNo(Math.addExact(previousNo, 1));
        attempt.setStatus("IN_PROGRESS");
        attempt.setTotalScore(totalScore);
        attempt.setStartedAt(now());
        // 插入后 MyBatis 通过 generated keys 给 attempt.id 赋值，后续所有明细都引用它。
        exams.insertAttempt(attempt);
        int position = 1;
        for (Question question : current) {
            // 快照属于这次考试。之后教师改题或逻辑删除题目，不会改写已经开出的卷。
            AttemptItem item = snapshot(question, attempt.getId(), position++);
            exams.insertItem(item);
        }
        return new StartAttemptResponse(attempt, false);
    }

    /** 只读本人答卷；仅 IN_PROGRESS 返回 StudentItemResponse 列表，已交卷 items 为空并提供结果路径。 */
    public AttemptResponse detail(SessionUser student, Long attemptId) {
        Attempt attempt = owned(student, attemptId);
        List<StudentItemResponse> items = "IN_PROGRESS".equals(attempt.getStatus())
                ? exams.items(attemptId).stream().map(StudentItemResponse::new).collect(Collectors.toList())
                : Collections.emptyList();
        // 单独的学生响应类没有正确答案字段，避免序列化时泄露标准答案。
        return new AttemptResponse(attempt, items);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    /** 事务性交卷：锁本人答卷 -> 已交卷返回原结果 -> 校验判分 -> 保存全部明细 -> 保存总分状态时间；异常向外抛触发回滚。 */
    public ResultResponse submit(SessionUser student, Long attemptId, SubmitAttemptRequest request) {
        access.requireRole(student, "STUDENT");
        // SQL 同时使用 attemptId 和 Session 中的 studentId，并加 FOR UPDATE；既检查归属又串行交卷。
        Attempt attempt = exams.lockOwned(attemptId, student.id);
        if (attempt == null) {
            throw BusinessException.notFound();
        }
        // 锁定读只查答卷表，因此额外补课程显示名；课程名不是本项目快照的一部分。
        attempt.setCourseName(courses.findById(attempt.getCourseId()).getName());
        if ("SUBMITTED".equals(attempt.getStatus())) {
            // 第二次提交不能按新答案重算，否则重试可被用来改分；直接返回第一次的持久化结果。
            return new ResultResponse(attempt);
        }
        List<AttemptItem> items = exams.items(attemptId);
        // request 没有 score 字段；逐项校验并使用数据库快照中的标准答案判分。
        GradingService.GradeResult result = grading.grade(items, request.getAnswers());
        saveGradedAnswers(items, result);
        // 最后更新答卷头；明细与总分共同提交，避免一半题已判分、卷头仍未提交的中间状态落库。
        attempt.setScore(result.score);
        attempt.setStatus("SUBMITTED");
        attempt.setSubmittedAt(now());
        if (exams.submit(attempt) != 1) {
            throw new IllegalStateException("Attempt update count mismatch");
        }
        // 如果任一写入失败，异常离开代理方法，所有明细和总成绩一并回滚。
        return new ResultResponse(attempt);
    }

    /**
     * 只负责保存每题结果。它在 submit 已开启的事务中执行，不需要单独标注 @Transactional。
     * 异常继续抛回 submit，让已经更新的题目明细和总成绩一起回滚。
     */
    /** 在 submit 已存在的事务中按题保存选择和得分；更新行数不等于 1 则抛错，不吞异常，不另开事务。 */
    private void saveGradedAnswers(List<AttemptItem> items, GradingService.GradeResult result) {
        for (AttemptItem item : items) {
            item.setChosenOption(result.selected.get(item.getId()));
            item.setEarnedPoints(result.points.get(item.getId()));
            if (exams.saveAnswer(item) != 1) {
                throw new IllegalStateException("Answer update count mismatch");
            }
        }
    }

    /** 查本人已交卷成绩；进行中答卷没有最终成绩，抛 NOT_SUBMITTED/409。 */
    public ResultResponse result(SessionUser student, Long attemptId) {
        Attempt attempt = owned(student, attemptId);
        if (!"SUBMITTED".equals(attempt.getStatus())) {
            throw BusinessException.conflict("NOT_SUBMITTED", "答卷尚未提交");
        }
        return new ResultResponse(attempt);
    }

    /** 学生角色加所有权查询；findOwned 的 SQL 同时使用答卷 ID 与 Session 学生 ID，查他人返回同样的 404。 */
    private Attempt owned(SessionUser student, Long attemptId) {
        access.requireRole(student, "STUDENT");
        // 对无权访问的他人答卷返回 404，既阻止读取，也尽量不暴露该编号是否存在。
        Attempt attempt = exams.findOwned(attemptId, student.id);
        if (attempt == null) {
            throw BusinessException.notFound();
        }
        return attempt;
    }

    /** 按 Asia/Shanghai 生成服务端 LocalDateTime 并去掉纳秒，匹配 MySQL DATETIME 与 JSON 秒级显示。 */
    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai")).withNano(0);
    }

    /** 把题库当前题干、选项、标准答案和分值复制到本次答卷明细；sourceQuestionId 只作来源，不作为以后判分时的读题入口。 */
    private AttemptItem snapshot(Question question, Long attemptId, int position) {
        AttemptItem item = new AttemptItem();
        item.setAttemptId(attemptId);
        item.setSourceQuestionId(question.getId());
        item.setPositionNo(position);
        item.setStemSnapshot(question.getStem());
        item.setOptionASnapshot(question.getOptionA());
        item.setOptionBSnapshot(question.getOptionB());
        item.setOptionCSnapshot(question.getOptionC());
        item.setOptionDSnapshot(question.getOptionD());
        item.setCorrectOptionSnapshot(question.getCorrectOption());
        item.setPointsSnapshot(question.getPoints());
        return item;
    }
}
