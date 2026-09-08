/* 文件导读：Spring 业务 Bean：从 MyBatis 读取账号，用 BCrypt 核验输入密码，返回账号对象供登录建立 Session；不把密码放入日志或响应。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.service;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.BusinessException;
import com.feidian.exam.dto.request.LoginRequest;
import com.feidian.exam.mapper.UserMapper;
import com.feidian.exam.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

/* 登录业务：读取账号、核验 BCrypt 哈希、检查角色；Session 的建立交给 SessionContext。 */
@Service
public class AuthService {
    /** 用户 Mapper 代理，执行账号/资料 SQL。 */
    private final UserMapper users;
    /** PasswordEncoder 接口，实际 Bean 是 BCryptPasswordEncoder。 */
    private final PasswordEncoder passwords;
    /** 未知用户也执行 BCrypt 核验所用虚拟哈希，不是一个真实登录账号。 */
    private final String dummyHash;

    /** 注入 UserMapper 与 PasswordEncoder，并准备虚拟密码哈希，供未知用户名也执行一次昂贵核验以降低耗时差异。 */
    public AuthService(UserMapper users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
        // 不存在的账号也做一次 BCrypt 校验，降低通过明显耗时差异判断账号是否存在的风险。
        // 这只是缓解措施，不等于完全消除时序差异，也不能代替登录限流。
        this.dummyHash = passwords.encode("timing-only-dummy-password");
    }
    /** 先按 UTF-8 字节检查 BCrypt 的 72 字节边界，再查用户名、统一核验密码、检查允许角色；失败抛 400/401/403 对应异常。 */
    public User authenticate(LoginRequest request) {
        // BCrypt 的长度边界按 UTF-8 字节计算；中文字符不能简单按 String.length() 判断。
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw BusinessException.badRequest("密码不能超过 72 个 UTF-8 字节");
        }
        User user = users.findByUsername(request.getUsername());
        // BCrypt 是单向哈希：调用 matches 校验，不是把哈希“解密”为密码。
        boolean matched = passwords.matches(request.getPassword(), user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !matched) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "用户名或密码错误");
        }
        if (!"TEACHER".equals(user.getRole()) && !"STUDENT".equals(user.getRole())) {
            throw BusinessException.forbidden();
        }
        return user;
    }
}
