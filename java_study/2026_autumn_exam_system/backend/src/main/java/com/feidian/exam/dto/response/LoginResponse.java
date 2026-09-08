/* 文件导读：登录成功响应白名单：账号 ID、角色、姓名和新 CSRF Token。Session ID 通过 Cookie 传递，不把密码哈希放进响应。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.dto.response;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.model.User;

public class LoginResponse {
    /** id：当前记录的 MySQL 主键（BIGINT 对应 Long）。 */
    public final Long id;
    /** csrfToken：登录后新生成的写请求令牌，下一次写请求需携带它。 */
    public final String role, realName, csrfToken;

    /** 构造当前响应并显式复制允许输出的字段；不执行数据库查询或判分。 */
    public LoginResponse(User user, String csrfToken) {
        this.id = user.getId();
        this.role = user.getRole();
        this.realName = user.getRealName();
        this.csrfToken = csrfToken;
    }
}
