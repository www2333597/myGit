/* 文件导读：学生端 Spring MVC 路由。客户端传资源 ID/答案，学生身份只从 Session 获取；Controller 不接收客户端计算的最终分数。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.controller;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.*;
import com.feidian.exam.dto.request.*;
import com.feidian.exam.dto.response.*;
import com.feidian.exam.security.SessionContext;
import com.feidian.exam.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 学生接口：路径参数告诉我们操作哪张卷，服务端 Session 告诉我们是谁在操作。
 * 不接收客户端自报的 studentId 或最终分数，避免伪造身份和改分。
 */
@RestController
@RequestMapping("/api/student")
@Tag(name = "学生端")
public class ExamController {
    /** 个人资料与课程概览业务依赖。 */
    private final ProfileService profiles;
    /** ExamService 业务 Bean：实现开考、详情、交卷和查分；写流程通过 Spring 代理事务执行。 */
    private final ExamService exams;
    /** 已保存成绩的查询依赖。 */
    private final GradeService grades;
    /** 服务端 Session/CSRF 操作组件。 */
    private final SessionContext sessions;

    /** 注入课程概览、考试业务、历史查询和会话组件；HTTP 层不实现事务细节或 SQL。 */
    public ExamController(ProfileService profiles, ExamService exams, GradeService grades, SessionContext sessions) {
        this.profiles = profiles;
        this.exams = exams;
        this.grades = grades;
        this.sessions = sessions;
    }

    @GetMapping("/courses")
    @Operation(summary = "查询已选课程、考试状态和最近成绩")
    /** GET 已选课程概览：包含课程状态、进行中答卷 ID 和最近一次成绩。 */
    public ApiResponse<List<StudentCourseResponse>> courses(HttpServletRequest request) {
        return ApiResponse.ok(profiles.studentCourses(sessions.requireUser(request)));
    }

    @PostMapping(value = "/courses/{courseId}/attempts", consumes = "application/json")
    @Operation(summary = "首次开考或重考，已有进行中答卷则复用")
    /** POST FIRST/RETAKE：PathVariable 是课程 ID，JSON 里只有模式；复用答卷返回 200，新建答卷返回 201。 */
    public ResponseEntity<ApiResponse<StartAttemptResponse>> start(@PathVariable Long courseId,
            @Valid @RequestBody StartAttemptRequest body, HttpServletRequest request) {
        StartAttemptResponse result = exams.start(sessions.requireUser(request), courseId, body.getMode());
        // 新建资源返回 201；复用已有进行中答卷返回 200。两者不是重复创建两张卷。
        return ResponseEntity.status(result.reused ? 200 : 201).body(ApiResponse.ok(result));
    }

    @GetMapping("/attempts/{attemptId}")
    @Operation(summary = "获取本人试卷，不包含正确答案")
    /** GET 本人答卷：Service 按学生 ID 和答卷 ID 检查归属；进行中返回不含答案的题目列表，已交卷返回结果入口。 */
    public ApiResponse<AttemptResponse> detail(@PathVariable Long attemptId, HttpServletRequest request) {
        return ApiResponse.ok(exams.detail(sessions.requireUser(request), attemptId));
    }

    @PostMapping(value = "/attempts/{attemptId}/submit", consumes = "application/json")
    @Operation(summary = "交卷并判分，重复提交返回原结果")
    /** POST 答案列表：DTO 校验后调用事务性 ExamService.submit；这里只转发经过认证的身份和输入，不自行判分。 */
    public ApiResponse<ResultResponse> submit(@PathVariable Long attemptId,
            @Valid @RequestBody SubmitAttemptRequest body, HttpServletRequest request) {
        // 事务放在 Service：无论 HTTP 请求还是集成测试调用，都复用同一套交卷规则。
        return ApiResponse.ok(exams.submit(sessions.requireUser(request), attemptId, body));
    }

    @GetMapping("/attempts/{attemptId}/result")
    @Operation(summary = "查看本人已交卷成绩")
    /** GET 本人已交卷结果：未交卷时返回 409，查别人答卷返回 404；成功包含本次得分和满分。 */
    public ApiResponse<ResultResponse> result(@PathVariable Long attemptId, HttpServletRequest request) {
        return ApiResponse.ok(exams.result(sessions.requireUser(request), attemptId));
    }

    @GetMapping("/courses/{courseId}/attempts")
    @Operation(summary = "分页查询本人本课程的历史成绩")
    /** GET 本人当前课程历史：课程需已选，页码经 PageQuery 校验，SQL 只查 SUBMITTED 并按考试次数倒序。 */
    public ApiResponse<PageResponse<GradeResponse>> history(@PathVariable Long courseId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.ok(grades.studentHistory(sessions.requireUser(request), courseId, new PageQuery(page, pageSize)));
    }
}
