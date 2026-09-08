/* 文件导读：MyBatis 答卷明细对象，对应 exam_attempt_item。题库 ID 只用于追溯来源，判分使用开考时复制的 snapshot 字段；chosenOption/earnedPoints 交卷时写入。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.model;

public class AttemptItem {
    /** id：当前记录的 MySQL 主键（BIGINT 对应 Long）。 */
    private Long id;
    /** attemptId：本次考试 exam_attempt.id。 */
    private Long attemptId;
    /** sourceQuestionId：来源题库 question.id，用于追溯而不是作为交卷 itemId。 */
    private Long sourceQuestionId;
    /** positionNo：本次试卷中的题序，从 1 开始。 */
    private Integer positionNo;
    /** stemSnapshot：开考时的题干副本。 */
    private String stemSnapshot;
    /** optionASnapshot：开考时的 A 选项副本。 */
    private String optionASnapshot;
    /** optionBSnapshot：开考时的 B 选项副本。 */
    private String optionBSnapshot;
    /** optionCSnapshot：开考时的 C 选项副本。 */
    private String optionCSnapshot;
    /** optionDSnapshot：开考时的 D 选项副本。 */
    private String optionDSnapshot;
    // Jackson 的 @JsonIgnore 排除 JSON 字段；MyBatis 仍能读取/写入该属性，两者职责不同。
    @com.fasterxml.jackson.annotation.JsonIgnore
    /** correctOptionSnapshot：开考时的标准答案，只有服务端判分可用。 */
    private String correctOptionSnapshot;
    /** pointsSnapshot：开考时单题满分，教师改题不改变它。 */
    private Integer pointsSnapshot;
    /** chosenOption：学生最终选择；漏答为 null。 */
    private String chosenOption;
    /** earnedPoints：这道题最终所得分；未交卷为 null、漏答或答错为 0。 */
    private Integer earnedPoints;

    /** 读取当前记录的 MySQL 主键（BIGINT 对应 Long）；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Long getId() { return id; }
    /** 设置当前记录的 MySQL 主键（BIGINT 对应 Long）；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setId(Long id) { this.id = id; }

    /** 读取本次考试 exam_attempt.id；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Long getAttemptId() { return attemptId; }
    /** 设置本次考试 exam_attempt.id；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }

    /** 读取来源题库 question.id，用于追溯而不是作为交卷 itemId；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Long getSourceQuestionId() { return sourceQuestionId; }
    /** 设置来源题库 question.id，用于追溯而不是作为交卷 itemId；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setSourceQuestionId(Long sourceQuestionId) { this.sourceQuestionId = sourceQuestionId; }

    /** 读取本次试卷中的题序，从 1 开始；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Integer getPositionNo() { return positionNo; }
    /** 设置本次试卷中的题序，从 1 开始；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setPositionNo(Integer positionNo) { this.positionNo = positionNo; }

    /** 读取开考时的题干副本；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getStemSnapshot() { return stemSnapshot; }
    /** 设置开考时的题干副本；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setStemSnapshot(String stemSnapshot) { this.stemSnapshot = stemSnapshot; }

    /** 读取开考时的 A 选项副本；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getOptionASnapshot() { return optionASnapshot; }
    /** 设置开考时的 A 选项副本；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setOptionASnapshot(String optionASnapshot) { this.optionASnapshot = optionASnapshot; }

    /** 读取开考时的 B 选项副本；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getOptionBSnapshot() { return optionBSnapshot; }
    /** 设置开考时的 B 选项副本；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setOptionBSnapshot(String optionBSnapshot) { this.optionBSnapshot = optionBSnapshot; }

    /** 读取开考时的 C 选项副本；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getOptionCSnapshot() { return optionCSnapshot; }
    /** 设置开考时的 C 选项副本；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setOptionCSnapshot(String optionCSnapshot) { this.optionCSnapshot = optionCSnapshot; }

    /** 读取开考时的 D 选项副本；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getOptionDSnapshot() { return optionDSnapshot; }
    /** 设置开考时的 D 选项副本；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setOptionDSnapshot(String optionDSnapshot) { this.optionDSnapshot = optionDSnapshot; }

    /** 读取开考时的标准答案，只有服务端判分可用；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getCorrectOptionSnapshot() { return correctOptionSnapshot; }
    /** 设置开考时的标准答案，只有服务端判分可用；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setCorrectOptionSnapshot(String correctOptionSnapshot) { this.correctOptionSnapshot = correctOptionSnapshot; }

    /** 读取开考时单题满分，教师改题不改变它；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Integer getPointsSnapshot() { return pointsSnapshot; }
    /** 设置开考时单题满分，教师改题不改变它；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setPointsSnapshot(Integer pointsSnapshot) { this.pointsSnapshot = pointsSnapshot; }

    /** 读取学生最终选择；漏答为 null；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public String getChosenOption() { return chosenOption; }
    /** 设置学生最终选择；漏答为 null；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setChosenOption(String chosenOption) { this.chosenOption = chosenOption; }

    /** 读取这道题最终所得分；未交卷为 null、漏答或答错为 0；供业务代码/Jackson 或 MyBatis 获取属性值。 */
    public Integer getEarnedPoints() { return earnedPoints; }
    /** 设置这道题最终所得分；未交卷为 null、漏答或答错为 0；由 MyBatis 映射或业务层赋值，此方法本身不执行 SQL。 */
    public void setEarnedPoints(Integer earnedPoints) { this.earnedPoints = earnedPoints; }
}
