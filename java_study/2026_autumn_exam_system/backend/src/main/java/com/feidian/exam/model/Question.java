/* 文件导读：MyBatis 题库对象：保存当前版本的单选题。deleted 对应逻辑删除标志；created_at/updated_at 由数据库管理，当前 Java 对象没有映射这两个字段。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.model;

public class Question {
    /** id：当前记录的 MySQL 主键（BIGINT 对应 Long）。 */
    private Long id;
    /** courseId：所属课程 course.id。 */
    private Long courseId;
    /** stem：题干文本。 */
    private String stem;
    /** optionA：A 选项文本。 */
    private String optionA;
    /** optionB：B 选项文本。 */
    private String optionB;
    /** optionC：C 选项文本。 */
    private String optionC;
    /** optionD：D 选项文本。 */
    private String optionD;
    /** correctOption：标准答案字母 A/B/C/D。 */
    private String correctOption;
    /** points：单题正整数分值（1–100）。 */
    private Integer points;
    /** deleted：逻辑删除标志，true 时不参加新考试。 */
    private Boolean deleted;

    /** 读取当前记录的 MySQL 主键（BIGINT 对应 Long）；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Long getId() { return id; }
    /** 设置当前记录的 MySQL 主键（BIGINT 对应 Long）；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setId(Long id) { this.id = id; }

    /** 读取所属课程 course.id；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Long getCourseId() { return courseId; }
    /** 设置所属课程 course.id；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    /** 读取题干文本；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getStem() { return stem; }
    /** 设置题干文本；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setStem(String stem) { this.stem = stem; }

    /** 读取A 选项文本；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getOptionA() { return optionA; }
    /** 设置A 选项文本；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setOptionA(String optionA) { this.optionA = optionA; }

    /** 读取B 选项文本；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getOptionB() { return optionB; }
    /** 设置B 选项文本；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setOptionB(String optionB) { this.optionB = optionB; }

    /** 读取C 选项文本；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getOptionC() { return optionC; }
    /** 设置C 选项文本；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setOptionC(String optionC) { this.optionC = optionC; }

    /** 读取D 选项文本；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getOptionD() { return optionD; }
    /** 设置D 选项文本；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setOptionD(String optionD) { this.optionD = optionD; }

    /** 读取标准答案字母 A/B/C/D；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getCorrectOption() { return correctOption; }
    /** 设置标准答案字母 A/B/C/D；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }

    /** 读取单题正整数分值（1–100）；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Integer getPoints() { return points; }
    /** 设置单题正整数分值（1–100）；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setPoints(Integer points) { this.points = points; }

    /** 读取逻辑删除标志，true 时不参加新考试；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Boolean getDeleted() { return deleted; }
    /** 设置逻辑删除标志，true 时不参加新考试；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
