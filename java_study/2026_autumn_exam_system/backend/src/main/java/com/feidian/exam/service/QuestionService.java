/* 文件导读：题库业务 Bean：所有读写先验证课程归属，写操作在 Spring 事务内完成。MyBatis XML 决定 SQL，逻辑删除保留历史答卷引用。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.service;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.*;
import com.feidian.exam.dto.request.QuestionRequest;
import com.feidian.exam.mapper.QuestionMapper;
import com.feidian.exam.model.Question;
import com.feidian.exam.security.SessionUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 题库业务：先检查课程归属，再调用 Mapper；题库内容与已开考的试题快照相互独立。 */
@Service
public class QuestionService {
    /** QuestionMapper 代理：执行题库查询、插入、修改和逻辑删除，业务授权仍由本类负责。 */
    private final QuestionMapper questions;
    /** 课程角色与资源归属检查组件。 */
    private final CourseAccess access;

    /** 构造器注入题库 Mapper 和授权组件。 */
    public QuestionService(QuestionMapper questions, CourseAccess access) {
        this.questions = questions;
        this.access = access;
    }

    /** 验证本人课程后执行一条列表 SQL 和一条 COUNT SQL；两者采用一致的课程/deleted 条件。 */
    public PageResponse<Question> list(SessionUser teacher, Long courseId, PageQuery page) {
        access.teacherCourse(teacher, courseId);
        return new PageResponse<>(questions.list(courseId, page), questions.count(courseId), page);
    }

    /** 先按主键查未删除题，再用题目所属课程检查教师权限，防止只改 URL 编号越权。 */
    public Question detail(SessionUser teacher, Long questionId) {
        Question question = questions.findById(questionId);
        if (question == null) {
            throw BusinessException.notFound();
        }
        access.teacherCourse(teacher, question.getCourseId());
        return question;
    }

    @Transactional(rollbackFor = Exception.class)
    /** 验证课程、从白名单输入创建 Question、插入并回填 MySQL 自增主键，最后按 ID 回查完整题目。 */
    public Question create(SessionUser teacher, Long courseId, QuestionRequest request) {
        access.teacherCourse(teacher, courseId);
        Question question = from(request, courseId);
        questions.insert(question);
        // XML 的 useGeneratedKeys 把数据库生成的主键写回 question.id，随后可按主键回查。
        return questions.findById(question.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    /** 读取原题并验证归属，将输入复制到新对象，沿用原课程和题目 ID，更新成功后回查。 */
    public Question update(SessionUser teacher, Long questionId, QuestionRequest request) {
        // 先查原题并验证归属；课程 ID 沿用数据库原值，不能借更新请求把题目搬到别人的课程。
        Question existing = detail(teacher, questionId);
        Question updated = from(request, existing.getCourseId());
        updated.setId(existing.getId());
        if (questions.update(updated) != 1) {
            throw BusinessException.notFound();
        }
        return questions.findById(questionId);
    }

    @Transactional(rollbackFor = Exception.class)
    /** 先确认原题及归属，再把 deleted 标为 true；更新行数不为 1 时报告不存在。 */
    public void delete(SessionUser teacher, Long questionId) {
        Question existing = detail(teacher, questionId);
        // Mapper 中实际执行 UPDATE deleted=TRUE，不是物理 DELETE；旧卷仍保留历史来源。
        if (questions.delete(questionId, existing.getCourseId()) != 1) {
            throw BusinessException.notFound();
        }
    }

    /** 将允许字段逐项复制并 trim 文本；不接受客户端 courseId/id/deleted 等内部状态。 */
    private Question from(QuestionRequest input, Long courseId) {
        // 显式复制允许修改的字段，避免把客户端整个对象直接当数据库记录覆盖。
        Question result = new Question();
        result.setCourseId(courseId);
        result.setStem(input.getStem().trim());
        result.setOptionA(input.getOptionA().trim());
        result.setOptionB(input.getOptionB().trim());
        result.setOptionC(input.getOptionC().trim());
        result.setOptionD(input.getOptionD().trim());
        result.setCorrectOption(input.getCorrectOption());
        result.setPoints(input.getPoints());
        return result;
    }
}
