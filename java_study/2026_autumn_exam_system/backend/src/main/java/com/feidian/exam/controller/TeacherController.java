/* 文件导读：教师端 Spring MVC 路由集合。PathVariable 取课程/题目 ID，RequestParam 取查询筛选和页码，RequestBody 取 JSON。教师角色由拦截器检查，具体课程归属由 Service 检查。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.controller;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.*;
import com.feidian.exam.dto.request.QuestionRequest;
import com.feidian.exam.dto.response.GradeResponse;
import com.feidian.exam.model.*;
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
 * 教师接口只做请求适配；QuestionService/GradeService 继续验证课程是否属于该教师。
 * “能进入教师接口”不等于“能修改任意教师的题目”，两层权限检查不能互相替代。
 */
@RestController
@RequestMapping("/api/teacher")
@Tag(name = "教师端")
public class TeacherController {
    /** 个人资料与课程概览业务依赖。 */
    private final ProfileService profiles;
    /** QuestionService 业务 Bean：接收已绑定的 HTTP 参数，检查课程归属后操作题库。 */
    private final QuestionService questions;
    /** 已保存成绩的查询依赖。 */
    private final GradeService grades;
    /** 服务端 Session/CSRF 操作组件。 */
    private final SessionContext sessions;

    /** 构造器注入资料、题库、成绩业务和会话组件；这些引用可跨请求复用，具体用户信息只留在方法局部变量。 */
    public TeacherController(ProfileService profiles, QuestionService questions, GradeService grades, SessionContext sessions) {
        this.profiles = profiles;
        this.questions = questions;
        this.grades = grades;
        this.sessions = sessions;
    }

    @GetMapping("/courses")
    @Operation(summary = "查询本人教授的课程")
    /** GET 本人授课列表：Session ID 限制 teacher_id，学生身份不能进入该路径。 */
    public ApiResponse<List<Course>> courses(HttpServletRequest request) {
        return ApiResponse.ok(profiles.teacherCourses(sessions.requireUser(request)));
    }

    @GetMapping("/courses/{courseId}/questions")
    @Operation(summary = "分页查询课程题目")
    /** GET 课程题目页：先构造 PageQuery 验证范围，再由 QuestionService 验证课程归属并查询列表和总数。 */
    public ApiResponse<PageResponse<Question>> questions(@PathVariable Long courseId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.ok(questions.list(sessions.requireUser(request), courseId, new PageQuery(page, pageSize)));
    }

    @GetMapping("/questions/{questionId}")
    @Operation(summary = "查看题目详情和标准答案")
    /** GET 单题详情：题目必须存在且属于本人课程；这个教师响应可含标准答案，不能复用于学生答卷。 */
    public ApiResponse<Question> question(@PathVariable Long questionId, HttpServletRequest request) {
        return ApiResponse.ok(questions.detail(sessions.requireUser(request), questionId));
    }

    @PostMapping(value = "/courses/{courseId}/questions", consumes = "application/json")
    @Operation(summary = "新增课程题目")
    /** POST 新题：绑定并校验完整 QuestionRequest，由 Service 指定课程及插入，HTTP 201 表示创建成功。 */
    public ResponseEntity<ApiResponse<Question>> create(@PathVariable Long courseId,
            @Valid @RequestBody QuestionRequest body, HttpServletRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(questions.create(sessions.requireUser(request), courseId, body)));
    }

    @PutMapping(value = "/questions/{questionId}", consumes = "application/json")
    @Operation(summary = "修改题目")
    /** PUT 修改题：必须提交完整题目输入；Service 使用原题所属课程，拒绝借更新转移资源归属。 */
    public ApiResponse<Question> update(@PathVariable Long questionId,
            @Valid @RequestBody QuestionRequest body, HttpServletRequest request) {
        return ApiResponse.ok(questions.update(sessions.requireUser(request), questionId, body));
    }

    @DeleteMapping("/questions/{questionId}")
    @Operation(summary = "逻辑删除题目，保留历史快照")
    /** DELETE 题目接口：调用的 SQL 实际是 UPDATE deleted=TRUE，成功返回 data=null；历史快照不受影响。 */
    public ApiResponse<Void> delete(@PathVariable Long questionId, HttpServletRequest request) {
        questions.delete(sessions.requireUser(request), questionId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/courses/{courseId}/grades")
    @Operation(summary = "分页查询每位学生最近一次成绩")
    /** GET 最近成绩：支持可选 studentName 与分页，返回每名已交卷学生最近一次记录，不是最高分或未考学生名单。 */
    public ApiResponse<PageResponse<GradeResponse>> grades(@PathVariable Long courseId,
            @RequestParam(required = false) String studentName,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.ok(grades.latest(sessions.requireUser(request), courseId, studentName, new PageQuery(page, pageSize)));
    }

    @GetMapping("/courses/{courseId}/students/{studentId}/attempts")
    @Operation(summary = "查询指定学生本课程的历史成绩")
    /** GET 指定学生在本人课程的全部已交卷历史；教师身份不意味着可以访问其他教师课程。 */
    public ApiResponse<PageResponse<GradeResponse>> history(@PathVariable Long courseId, @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.ok(grades.teacherHistory(sessions.requireUser(request), courseId, studentId, new PageQuery(page, pageSize)));
    }
}
