/* 文件导读：成绩查询响应，也是 MyBatis 联表查询的结果类型。XML 列别名和驼峰映射把学生/课程/答卷数据写进这个 JavaBean；Jackson 再通过 getter 输出 JSON。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.dto.response;

public class GradeResponse {
    /** studentId：学生 sys_user.id。 */
    private Long studentId;
    /** studentName：联表查得的学生姓名。 */
    private String studentName;
    /** identityNo：学生学号或教师工号（不是身份证号）。 */
    private String identityNo;
    /** courseId：所属课程 course.id。 */
    private Long courseId;
    /** courseName：课程显示名称。 */
    private String courseName;
    /** attemptId：本次考试 exam_attempt.id。 */
    private Long attemptId;
    /** attemptNo：该学生该课程的考试次数序号，从 1 开始。 */
    private Integer attemptNo;
    /** score：实际得分，未交卷时为 null，0 分表示已经判分但没有得分。 */
    private Integer score;
    /** totalScore：开考时整张试卷的满分，不固定为 100。 */
    private Integer totalScore;
    // @JsonFormat 控制 LocalDateTime 的 JSON 字符串格式；类型自身不保存时区信息。
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** startedAt：服务端记录的开考时间，本项目按北京时间处理。 */
    private java.time.LocalDateTime startedAt;
    // @JsonFormat 控制 LocalDateTime 的 JSON 字符串格式；类型自身不保存时区信息。
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** submittedAt：第一次成功交卷的时间，重复提交不更新它。 */
    private java.time.LocalDateTime submittedAt;

    /** 读取学生 sys_user.id；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Long getStudentId() { return studentId; }
    /** 设置学生 sys_user.id；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    /** 读取联表查得的学生姓名；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getStudentName() { return studentName; }
    /** 设置联表查得的学生姓名；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setStudentName(String studentName) { this.studentName = studentName; }

    /** 读取学生学号或教师工号（不是身份证号）；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getIdentityNo() { return identityNo; }
    /** 设置学生学号或教师工号（不是身份证号）；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setIdentityNo(String identityNo) { this.identityNo = identityNo; }

    /** 读取所属课程 course.id；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Long getCourseId() { return courseId; }
    /** 设置所属课程 course.id；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    /** 读取课程显示名称；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getCourseName() { return courseName; }
    /** 设置课程显示名称；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setCourseName(String courseName) { this.courseName = courseName; }

    /** 读取本次考试 exam_attempt.id；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Long getAttemptId() { return attemptId; }
    /** 设置本次考试 exam_attempt.id；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }

    /** 读取该学生该课程的考试次数序号，从 1 开始；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Integer getAttemptNo() { return attemptNo; }
    /** 设置该学生该课程的考试次数序号，从 1 开始；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setAttemptNo(Integer attemptNo) { this.attemptNo = attemptNo; }

    /** 读取实际得分，未交卷时为 null，0 分表示已经判分但没有得分；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Integer getScore() { return score; }
    /** 设置实际得分，未交卷时为 null，0 分表示已经判分但没有得分；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setScore(Integer score) { this.score = score; }

    /** 读取开考时整张试卷的满分，不固定为 100；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Integer getTotalScore() { return totalScore; }
    /** 设置开考时整张试卷的满分，不固定为 100；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }

    /** 读取服务端记录的开考时间，本项目按北京时间处理；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public java.time.LocalDateTime getStartedAt() { return startedAt; }
    /** 设置服务端记录的开考时间，本项目按北京时间处理；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setStartedAt(java.time.LocalDateTime startedAt) { this.startedAt = startedAt; }

    /** 读取第一次成功交卷的时间，重复提交不更新它；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public java.time.LocalDateTime getSubmittedAt() { return submittedAt; }
    /** 设置第一次成功交卷的时间，重复提交不更新它；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setSubmittedAt(java.time.LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
