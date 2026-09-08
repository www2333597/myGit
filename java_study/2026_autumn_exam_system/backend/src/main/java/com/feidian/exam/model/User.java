/* 文件导读：MyBatis 数据对象，对应 MySQL sys_user 表。没有 @Entity，因为这里不是 JPA。无显式构造器时 Java 提供无参构造，MyBatis 创建对象后映射字段。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.model;

public class User {
    /** id：当前记录的 MySQL 主键（BIGINT 对应 Long）。 */
    private Long id;
    /** username：登录账号（3–32 位字母、数字、下划线）。 */
    private String username;
    // Jackson 的 @JsonIgnore 排除 JSON 字段；MyBatis 仍能读取/写入该属性，两者职责不同。
    @com.fasterxml.jackson.annotation.JsonIgnore
    /** passwordHash：BCrypt 密码哈希，不能当明文解密或返回客户端。 */
    private String passwordHash;
    /** role：角色 TEACHER/STUDENT，真正授权依据来自服务端。 */
    private String role;
    /** realName：真实姓名/演示姓名。 */
    private String realName;
    /** gender：性别枚举 UNKNOWN/MALE/FEMALE。 */
    private String gender;
    /** phone：电话号码文本，保留前导零和加号。 */
    private String phone;
    /** identityNo：学生学号或教师工号（不是身份证号）。 */
    private String identityNo;
    /** college：所属学院。 */
    private String college;

    /** 读取当前记录的 MySQL 主键（BIGINT 对应 Long）；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Long getId() { return id; }
    /** 设置当前记录的 MySQL 主键（BIGINT 对应 Long）；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setId(Long id) { this.id = id; }

    /** 读取登录账号（3–32 位字母、数字、下划线）；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getUsername() { return username; }
    /** 设置登录账号（3–32 位字母、数字、下划线）；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setUsername(String username) { this.username = username; }

    /** 读取BCrypt 密码哈希，不能当明文解密或返回客户端；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getPasswordHash() { return passwordHash; }
    /** 设置BCrypt 密码哈希，不能当明文解密或返回客户端；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /** 读取角色 TEACHER/STUDENT，真正授权依据来自服务端；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getRole() { return role; }
    /** 设置角色 TEACHER/STUDENT，真正授权依据来自服务端；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setRole(String role) { this.role = role; }

    /** 读取真实姓名/演示姓名；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getRealName() { return realName; }
    /** 设置真实姓名/演示姓名；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setRealName(String realName) { this.realName = realName; }

    /** 读取性别枚举 UNKNOWN/MALE/FEMALE；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getGender() { return gender; }
    /** 设置性别枚举 UNKNOWN/MALE/FEMALE；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setGender(String gender) { this.gender = gender; }

    /** 读取电话号码文本，保留前导零和加号；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getPhone() { return phone; }
    /** 设置电话号码文本，保留前导零和加号；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setPhone(String phone) { this.phone = phone; }

    /** 读取学生学号或教师工号（不是身份证号）；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getIdentityNo() { return identityNo; }
    /** 设置学生学号或教师工号（不是身份证号）；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setIdentityNo(String identityNo) { this.identityNo = identityNo; }

    /** 读取所属学院；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getCollege() { return college; }
    /** 设置所属学院；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setCollege(String college) { this.college = college; }
}
