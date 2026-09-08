/* 文件导读：MyBatis 课程对象：id/name/teacherId/description 来自 course；teacherName 是 JOIN sys_user 的查询别名，不是 course 表里的物理字段。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.model;

public class Course {
    /** id：当前记录的 MySQL 主键（BIGINT 对应 Long）。 */
    private Long id;
    /** name：课程名称。 */
    private String name;
    /** teacherId：授课教师 sys_user.id。 */
    private Long teacherId;
    /** teacherName：联表查得的教师姓名。 */
    private String teacherName;
    /** description：课程说明。 */
    private String description;

    /** 读取当前记录的 MySQL 主键（BIGINT 对应 Long）；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Long getId() { return id; }
    /** 设置当前记录的 MySQL 主键（BIGINT 对应 Long）；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setId(Long id) { this.id = id; }

    /** 读取课程名称；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getName() { return name; }
    /** 设置课程名称；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setName(String name) { this.name = name; }

    /** 读取授课教师 sys_user.id；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Long getTeacherId() { return teacherId; }
    /** 设置授课教师 sys_user.id；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    /** 读取联表查得的教师姓名；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getTeacherName() { return teacherName; }
    /** 设置联表查得的教师姓名；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    /** 读取课程说明；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getDescription() { return description; }
    /** 设置课程说明；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setDescription(String description) { this.description = description; }
}
