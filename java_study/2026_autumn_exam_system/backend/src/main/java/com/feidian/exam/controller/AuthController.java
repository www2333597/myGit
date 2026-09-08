/* 文件导读：Spring MVC 认证入口。RequestMapping 提供 /api 前缀，Get/PostMapping 按 HTTP 方法匹配；Operation/Tag 只参与文档，RestController 才负责注册 JSON 接口。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.controller;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.ApiResponse;
import com.feidian.exam.dto.request.LoginRequest;
import com.feidian.exam.dto.response.LoginResponse;
import com.feidian.exam.model.User;
import com.feidian.exam.security.SessionContext;
import com.feidian.exam.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;

/*
 * HTTP 入口：接收请求、调用业务、返回 JSON；这里不编写 SQL，也不直接比较密码。
 * @RestController 让返回对象经过消息转换器输出为 JSON，而不是寻找 HTML 页面。
 */
@RestController
@RequestMapping("/api")
@Tag(name = "认证与健康检查")
public class AuthController {
    /** 认证业务/拦截器依赖，由 Spring 注入。 */
    private final AuthService auth;
    /** 服务端 Session/CSRF 操作组件。 */
    private final SessionContext sessions;

    // 构造器注入：所需对象由 Spring 提供。单构造器不必再写 @Autowired。
    /* 构造器注入业务认证和会话组件；Controller 自己不创建 Mapper 或数据库连接。 */
    public AuthController(AuthService auth, SessionContext sessions) {
        this.auth = auth;
        this.sessions = sessions;
    }

    @GetMapping("/health")
    @Operation(summary = "检查应用是否已启动")
    /* GET /api/health 返回 UP 常量，仅验证 HTTP 应用启动，不代表 MySQL 可用。 */
    public ApiResponse<Map<String, String>> health() {
        // 这里只证明 HTTP 应用活着；判断数据库连接应实际调用需要查询数据库的接口。
        return ApiResponse.ok(Collections.singletonMap("status", "UP"));
    }

    @GetMapping("/auth/csrf")
    @Operation(summary = "获取当前会话 CSRF Token")
    /* GET /api/auth/csrf 获取或建立匿名 Session 并返回令牌；调用方必须保留 Cookie，再携带令牌登录。 */
    public ApiResponse<Map<String, String>> csrf(HttpServletRequest request) {
        return ApiResponse.ok(Collections.singletonMap("csrfToken", sessions.csrfToken(request)));
    }

    @PostMapping(value = "/auth/login", consumes = "application/json")
    @Operation(summary = "登录并更新会话和 CSRF Token")
    /* POST JSON 经 RequestBody 绑定、Valid 校验后认证；成功时更新 Session ID/Token，只返回白名单 LoginResponse。 */
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        // @RequestBody 把 JSON 转成 DTO；@Valid 校验 DTO 注解，失败时不会执行下面的业务。
        User user = auth.authenticate(body);        // 登录响应使用专门 DTO，不返回包含 passwordHash 的数据库 User 对象。

        return ApiResponse.ok(new LoginResponse(user, sessions.login(request, user)));
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "退出登录")
    /* POST /api/auth/logout 使当前 Session 失效并返回无数据成功；登录和 CSRF 检查在拦截器先完成。 */
    public ApiResponse<Void> logout(HttpServletRequest request) {
        sessions.logout(request);
        return ApiResponse.ok(null);
    }
}
