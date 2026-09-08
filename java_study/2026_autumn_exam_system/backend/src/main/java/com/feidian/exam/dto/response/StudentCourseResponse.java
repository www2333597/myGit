/* 文件导读：学生课程概览 DTO：由 CourseMapper.studentOverview 联表与 CASE 计算得到；进行中答卷和最近已交卷成绩是两组独立字段，重考时可同时存在。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.dto.response;

public class StudentCourseResponse {
    /** courseId：所属课程 course.id。 */
    private Long courseId;
    /** courseName：课程显示名称。 */
    private String courseName;
    /** teacherName：联表查得的教师姓名。 */
    private String teacherName;
    /** examStatus：课程概览状态 NOT_TAKEN/IN_PROGRESS/COMPLETED。 */
    private String examStatus;
    /** activeAttemptId：当前进行中的答卷 ID，没有则为 null。 */
    private Long activeAttemptId;
    /** latestAttemptId：最近一次已交卷答卷 ID。 */
    private Long latestAttemptId;
    /** latestScore：最近一次已交卷得分，不是历史最高分。 */
    private Integer latestScore;
    /** latestTotalScore：最近一次已交卷对应的卷面满分。 */
    private Integer latestTotalScore;

    /** 读取所属课程 course.id；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Long getCourseId() { return courseId; }
    /** 设置所属课程 course.id；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    /** 读取课程显示名称；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getCourseName() { return courseName; }
    /** 设置课程显示名称；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setCourseName(String courseName) { this.courseName = courseName; }

    /** 读取联表查得的教师姓名；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getTeacherName() { return teacherName; }
    /** 设置联表查得的教师姓名；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    /** 读取课程概览状态 NOT_TAKEN/IN_PROGRESS/COMPLETED；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getExamStatus() { return examStatus; }
    /** 设置课程概览状态 NOT_TAKEN/IN_PROGRESS/COMPLETED；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setExamStatus(String examStatus) { this.examStatus = examStatus; }

    /** 读取当前进行中的答卷 ID，没有则为 null；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Long getActiveAttemptId() { return activeAttemptId; }
    /** 设置当前进行中的答卷 ID，没有则为 null；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setActiveAttemptId(Long activeAttemptId) { this.activeAttemptId = activeAttemptId; }

    /** 读取最近一次已交卷答卷 ID；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Long getLatestAttemptId() { return latestAttemptId; }
    /** 设置最近一次已交卷答卷 ID；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setLatestAttemptId(Long latestAttemptId) { this.latestAttemptId = latestAttemptId; }

    /** 读取最近一次已交卷得分，不是历史最高分；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Integer getLatestScore() { return latestScore; }
    /** 设置最近一次已交卷得分，不是历史最高分；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setLatestScore(Integer latestScore) { this.latestScore = latestScore; }

    /** 读取最近一次已交卷对应的卷面满分；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Integer getLatestTotalScore() { return latestTotalScore; }
    /** 设置最近一次已交卷对应的卷面满分；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setLatestTotalScore(Integer latestTotalScore) { this.latestTotalScore = latestTotalScore; }
}
