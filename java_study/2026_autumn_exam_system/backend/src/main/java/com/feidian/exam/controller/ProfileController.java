/* 文件导读：Spring MVC 的本人资料接口。所有身份来自已认证 Session，URL 和 JSON 不让调用方选择其他用户 ID；返回 UserResponse 排除密码哈希。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.controller;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.ApiResponse;
import com.feidian.exam.dto.request.ProfileUpdateRequest;
import com.feidian.exam.dto.response.UserResponse;
import com.feidian.exam.security.SessionContext;
import com.feidian.exam.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/me")
@Tag(name = "个人资料")
public class ProfileController {
    /** 个人资料与课程概览业务依赖。 */
    private final ProfileService profiles;
    /** 服务端 Session/CSRF 操作组件。 */
    private final SessionContext sessions;

    /** 注入资料 Service 与 SessionContext，将 HTTP 适配和数据库业务分离。 */
    public ProfileController(ProfileService profiles, SessionContext sessions) {
        this.profiles = profiles;
        this.sessions = sessions;
    }

    @GetMapping
    @Operation(summary = "获取本人资料和关联课程")
    /** GET /api/me：取得当前会话身份，查询本人资料及关联课程，再套统一 JSON 外壳。 */
    public ApiResponse<UserResponse> me(HttpServletRequest request) {
        return ApiResponse.ok(profiles.profile(sessions.requireUser(request)));
    }

    @PutMapping(consumes = "application/json")
    @Operation(summary = "更新姓名、性别、手机号和学院")
    /** PUT /api/me：JSON 白名单字段先校验，再由 Service 规范化并更新；不能修改角色、学工号或用户 ID。 */
    public ApiResponse<UserResponse> update(@Valid @RequestBody ProfileUpdateRequest body, HttpServletRequest request) {
        return ApiResponse.ok(profiles.update(sessions.requireUser(request), body));
    }
}
