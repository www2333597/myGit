/* 文件导读：Servlet HttpSession 操作组件，Spring 只负责注入它。Cookie 携带 Session ID，服务端会话保存身份和 CSRF；目前是单实例会话，不是 JWT 或 Redis 共享会话。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.security;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.BusinessException;
import com.feidian.exam.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import javax.servlet.http.*;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 浏览器 Cookie 保存会话 ID，用户 ID/角色与 CSRF Token 保存在服务端 HttpSession 中。
 * 当前未接入 Redis 等共享会话存储；重启后通常需要重新登录，不是 JWT 无状态认证。
 */
@Component
public class SessionContext {
    /** Session 中保存用户身份的属性名常量。 */
    private static final String USER = "exam.user";
    /** Session 中保存防跨站写请求令牌的属性名常量。 */
    private static final String CSRF = "exam.csrf";
    /** 安全随机数生成器，用于不可预测的 CSRF Token。 */
    private final SecureRandom random = new SecureRandom();

    /** getSession(false) 不创建会话；找不到身份就抛 401，Controller 不得信任请求体自报的用户 ID。 */
    public SessionUser requireUser(HttpServletRequest request) {
        // false 表示“没有就返回 null”，不为每个未登录访问新建一个空会话。
        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute(USER);
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "请先登录");
        }
        return user;
    }

    /** 必要时创建会话；在该 Session 对象锁内生成 32 字节安全随机数并做 URL 安全 Base64 编码，同一匿名会话可复用 Token。 */
    public String csrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        // 只保护本 JVM 中该会话令牌的生成；考试并发一致性使用数据库行锁，不靠这把锁。
        synchronized (session) {
            String token = (String) session.getAttribute(CSRF);
            if (token == null) {
                byte[] bytes = new byte[32];
                random.nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                session.setAttribute(CSRF, token);
            }
            return token;
        }
    }

    /** 只读取已经存在的 Token；写请求验证时不自动创建新 Token，防止无令牌请求被错误放行。 */
    public String existingCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(CSRF);
    }

    /** 认证后更新 Session ID、防会话固定；保存用户 ID/角色，移除旧 Token 并生成新 Token 返回给客户端。 */
    public String login(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        // 防止会话固定：认证成功后更新会话 ID 和 CSRF Token。
        request.changeSessionId();
        // 会话只保留认证必需的信息，不把密码、哈希或可随意修改的请求对象塞进 Session。
        session.setAttribute(USER, new SessionUser(user.getId(), user.getRole()));
        session.removeAttribute(CSRF);
        return csrfToken(request);
    }

    /** 使现有 Session 失效，清除该会话的登录态；没有会话时直接完成。 */
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
