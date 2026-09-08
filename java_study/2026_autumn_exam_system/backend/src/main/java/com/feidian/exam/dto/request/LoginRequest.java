/* 文件导读：登录请求 DTO：Spring MVC 的 Jackson 将 JSON 写入 username/password；@Valid 触发 Bean Validation，AuthService 再核验 BCrypt。密码仅用于本次核验，不得记录日志。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.dto.request;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import javax.validation.constraints.*;

public class LoginRequest {
    // @NotBlank 同时拒绝 null、空串及纯空白；与允许空串的 @NotNull 不同。
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,32}")
    /** username：登录账号（3–32 位字母、数字、下划线）。 */
    private String username;
    // @NotBlank 同时拒绝 null、空串及纯空白；与允许空串的 @NotNull 不同。
    @NotBlank @Size(max = 72)
    /** password：本次输入的明文登录密码；72 个字符约束之外还需检查 UTF-8 字节数。 */
    private String password;

    /** 读取登录账号（3–32 位字母、数字、下划线）；供 Service 取出已绑定的请求值。 */
    public String getUsername() { return username; }
    /** 设置登录账号（3–32 位字母、数字、下划线）；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setUsername(String username) { this.username = username; }

    /** 读取本次输入的明文登录密码；72 个字符约束之外还需检查 UTF-8 字节数；供 Service 取出已绑定的请求值。 */
    public String getPassword() { return password; }
    /** 设置本次输入的明文登录密码；72 个字符约束之外还需检查 UTF-8 字节数；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setPassword(String password) { this.password = password; }
}
