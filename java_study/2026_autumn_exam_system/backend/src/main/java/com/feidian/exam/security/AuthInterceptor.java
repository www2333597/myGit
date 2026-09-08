/* 文件导读：Spring MVC HandlerInterceptor：DispatcherServlet 找到处理器后，在 Controller 执行前完成检查。Component 注册 Bean，WebConfig.addInterceptors 决定真正的 URL 拦截范围。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.security;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 在 Controller 之前统一检查登录、接口角色和写请求令牌。
 * CSRF 针对浏览器自动携带 Cookie 的风险；令牌不是密码，也不能代替资源归属校验。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    /** 服务端 Session/CSRF 操作组件。 */
    private final SessionContext sessions;

    /** 通过构造器获取 SessionContext，共用会话规则而不重复存储当前用户。 */
    public AuthInterceptor(SessionContext sessions) {
        this.sessions = sessions;
    }

    @Override
    /** 设置禁止缓存等响应头，放行 OPTIONS 预检；其余请求先判公开路径/角色，再验证写请求 CSRF，任何失败抛异常交 MVC 处理。 */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getServletPath();
        // MockMvc 与真实 Servlet 容器都能取得准确的 API 路径。
        if (path.isEmpty()) {
            path = request.getRequestURI().substring(request.getContextPath().length());
        }
        String method = request.getMethod();
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        if ("OPTIONS".equals(method)) {
            // 浏览器跨域预检不代表业务操作；具体来源是否允许仍由 CORS 配置检查。
            return true;
        }
        boolean publicEndpoint = "/api/health".equals(path)
                || "/api/auth/csrf".equals(path) || "/api/auth/login".equals(path);
        if (!publicEndpoint) {
            SessionUser user = sessions.requireUser(request);
            // Java 的 && 优先于 ||：教师路径且非教师，或者学生路径且非学生，任一成立就拒绝。
            if (path.startsWith("/api/teacher/") && !"TEACHER".equals(user.role)
                    || path.startsWith("/api/student/") && !"STUDENT".equals(user.role)) {
                throw BusinessException.forbidden();
            }
        }
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            // 登录也是写请求：先 GET /api/auth/csrf，再带同一 Cookie 和令牌 POST 登录。
            // 登录会轮换令牌，后续写请求必须使用登录响应中的新值。
            String expected = sessions.existingCsrfToken(request);
            String supplied = request.getHeader("X-CSRF-Token");
            // 先检查 null 和长度，再比较字节；isEqual 用于安全令牌比较，不要改成忽略大小写。
            if (expected == null || supplied == null || supplied.length() > 100
                    || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    supplied.getBytes(StandardCharsets.UTF_8))) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "CSRF_INVALID", "请刷新会话令牌后重试");
            }
        }
        return true;
    }
}
