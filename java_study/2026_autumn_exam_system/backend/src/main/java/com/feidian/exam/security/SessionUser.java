/* 文件导读：Session 中保存的最小身份对象：用户 ID 与角色；Serializable 支持对象序列化，serialVersionUID 表示版本约定，不会自动实现分布式 Session 或数据加密。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.security;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import java.io.Serializable;

public class SessionUser implements Serializable {
    /** Java 序列化版本号约定，不是用户身份或密码。 */
    private static final long serialVersionUID = 1L;
    /** 认证成功后的用户数据库主键。 */
    public final Long id;
    /** 认证成功后的角色，由 Session 承载。 */
    public final String role;

    /** 认证成功时用数据库中的 ID/角色构造；字段 final，登录后的身份不能通过资料修改 DTO 改写。 */
    public SessionUser(Long id, String role) {
        this.id = id;
        this.role = role;
    }
}
