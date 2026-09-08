/* 文件导读：教师题目输入 DTO：题干/选项/标准答案/分值由 Bean Validation 先做格式校验；CourseAccess 验证课程归属，MySQL 约束作为最后防线。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.dto.request;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import javax.validation.constraints.*;

/**
 * 题目输入 DTO：Bean Validation 先拦截空文本、超长内容、非法选项与分值。
 * 数据库 CHECK/长度约束是第二道防线，两层校验服务于不同入口和不同失败阶段。
 */
public class QuestionRequest {
    // @NotBlank 同时拒绝 null、空串及纯空白；与允许空串的 @NotNull 不同。
    @NotBlank @Size(max = 2000)
    /* stem：题干文本。 */
    private String stem;
    // @NotBlank 同时拒绝 null、空串及纯空白；与允许空串的 @NotNull 不同。
    @NotBlank @Size(max = 500)
    /* optionA：A 选项文本。 */
    private String optionA;
    // @NotBlank 同时拒绝 null、空串及纯空白；与允许空串的 @NotNull 不同。
    @NotBlank @Size(max = 500)
    /* optionB：B 选项文本。 */
    private String optionB;
    // @NotBlank 同时拒绝 null、空串及纯空白；与允许空串的 @NotNull 不同。
    @NotBlank @Size(max = 500)
    /* optionC：C 选项文本。 */
    private String optionC;
    // @NotBlank 同时拒绝 null、空串及纯空白；与允许空串的 @NotNull 不同。
    @NotBlank @Size(max = 500)
    /* optionD：D 选项文本。 */
    private String optionD;
    // @NotNull 拒绝缺失答案；@Pattern 要求整个字符串恰好是 A/B/C/D 中的一个字母。
    @NotNull @Pattern(regexp = "[ABCD]")
    /* correctOption：标准答案字母 A/B/C/D。 */
    private String correctOption;
    // @NotNull 拒绝未提供分值；@Min/@Max 限定闭区间 1–100，0 分和超上限都不合法。
    @NotNull @Min(1) @Max(100)
    /* points：单题正整数分值（1–100）。 */
    private Integer points;

    /** 读取题干文本；供 Service 取出已绑定的请求值。 */
    public String getStem() { return stem; }
    /** 设置题干文本；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setStem(String stem) { this.stem = stem; }

    /** 读取A 选项文本；供 Service 取出已绑定的请求值。 */
    public String getOptionA() { return optionA; }
    /** 设置A 选项文本；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setOptionA(String optionA) { this.optionA = optionA; }

    /** 读取B 选项文本；供 Service 取出已绑定的请求值。 */
    public String getOptionB() { return optionB; }
    /** 设置B 选项文本；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setOptionB(String optionB) { this.optionB = optionB; }

    /** 读取C 选项文本；供 Service 取出已绑定的请求值。 */
    public String getOptionC() { return optionC; }
    /** 设置C 选项文本；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setOptionC(String optionC) { this.optionC = optionC; }

    /** 读取D 选项文本；供 Service 取出已绑定的请求值。 */
    public String getOptionD() { return optionD; }
    /** 设置D 选项文本；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setOptionD(String optionD) { this.optionD = optionD; }

    /** 读取标准答案字母 A/B/C/D；供 Service 取出已绑定的请求值。 */
    public String getCorrectOption() { return correctOption; }
    /** 设置标准答案字母 A/B/C/D；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }

    /** 读取单题正整数分值（1–100）；供 Service 取出已绑定的请求值。 */
    public Integer getPoints() { return points; }
    /** 设置单题正整数分值（1–100）；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setPoints(Integer points) { this.points = points; }
}
