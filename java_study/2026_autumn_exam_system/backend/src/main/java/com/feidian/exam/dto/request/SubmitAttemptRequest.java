/* 文件导读：交卷请求 DTO：列表及每个元素都需要校验，@Valid 递归进入 Answer；空列表表示全部漏答。Answer 的 itemId 是答卷明细主键，不是题库主键。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.dto.request;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * 交卷请求 DTO。客户端只能提交“试题明细 ID + 选项”，没有 studentId、正确答案或 score 字段。
 * application.yml 开启未知字段拒绝，因此多传 score 也会得到 400，而不是被静默接受。
 */
public class SubmitAttemptRequest {
    // 空列表合法，表示全部漏答；null 不合法；题数上限与开考上限一致。
    // @NotNull 要求列表存在；@Size 限制最多 1000 项；类型参数上的 @NotNull/@Valid 继续校验每个元素。
    @NotNull @Size(max = 1000)
    /** answers：学生答案列表，不含总分或标准答案。 */
    private List<@NotNull @Valid Answer> answers;

    /** 读取学生答案列表，不含总分或标准答案；供 Service 取出已绑定的请求值。 */
    public List<Answer> getAnswers() { return answers; }
    /** 设置学生答案列表，不含总分或标准答案；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setAnswers(List<Answer> answers) { this.answers = answers; }

    /** 一个答案元素；static 表示无需先创建外层请求对象即可构造，便于 JSON 绑定和单元测试。 */
    public static class Answer {
        // @NotNull 要求编号存在；@Positive 拒绝 0/负数，是否属于本卷仍需 Service 检查。
        @NotNull @Positive
        /** itemId：本张答卷的 exam_attempt_item.id；不是 question.id。 */
        private Long itemId;
        // 显式提交的选项不能为空且只能 A–D；漏答应省略整条 Answer，而不是提交 null 选项。
        @NotNull @Pattern(regexp = "[ABCD]")
        /** selectedOption：学生选择的单个选项字母。 */
        private String selectedOption;

        /** 读取本张答卷的 exam_attempt_item.id；不是 question.id；供 Service 取出已绑定的请求值。 */
        public Long getItemId() { return itemId; }
        /** 设置本张答卷的 exam_attempt_item.id；不是 question.id；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
        public void setItemId(Long itemId) { this.itemId = itemId; }
        /** 读取学生选择的单个选项字母；供 Service 取出已绑定的请求值。 */
        public String getSelectedOption() { return selectedOption; }
        /** 设置学生选择的单个选项字母；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
        public void setSelectedOption(String selectedOption) { this.selectedOption = selectedOption; }
    }
}
