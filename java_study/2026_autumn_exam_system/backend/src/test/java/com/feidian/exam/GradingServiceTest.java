/* 文件导读：JUnit 5 单元测试，直接 new GradingService，不启动 Spring 或连接数据库；每个断言验证一条判分/分页规则。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.*;
import com.feidian.exam.dto.request.SubmitAttemptRequest.Answer;
import com.feidian.exam.model.AttemptItem;
import com.feidian.exam.service.GradingService;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** 不启动 Spring、不连接数据库，只验证纯判分和分页边界，运行快且定位准确。 */
class GradingServiceTest {
    private final GradingService grading = new GradingService();

    /** 用例目的：两道快照题全部答对，断言总分 10+20=30。 */
    @Test void allCorrect() {
        assertThat(grading.grade(items(), Arrays.asList(answer(1L, "A"), answer(2L, "B"))).score).isEqualTo(30);
    }
    /** 用例目的：两题都答错，断言总分为 0，不产生负分。 */
    @Test void allWrong() {
        assertThat(grading.grade(items(), Arrays.asList(answer(1L, "C"), answer(2L, "D"))).score).isZero();
    }
    /** 用例目的：只答第一题应得 10 分，空答案列表应得 0 分；验证漏答规则。 */
    @Test void missingAnswersGetZero() {
        assertThat(grading.grade(items(), Collections.singletonList(answer(1L, "A"))).score).isEqualTo(10);
        assertThat(grading.grade(items(), Collections.emptyList()).score).isZero();
    }
    /** 用例目的：故意打乱答案顺序，断言按 itemId 匹配后的每题分数与总分，避免下标配对导致错判。 */
    @Test void answersAreMatchedByItemIdInsteadOfListPosition() {
        // 请求故意把第 2 题放在前面；答案必须按题目 ID 匹配，不能按数组下标对齐。
        GradingService.GradeResult result = grading.grade(items(),
                Arrays.asList(answer(2L, "B"), answer(1L, "D")));
        assertThat(result.score).isEqualTo(20);
        assertThat(result.selected).containsEntry(1L, "D").containsEntry(2L, "B");
        assertThat(result.points).containsEntry(1L, 0).containsEntry(2L, 20);
    }
    /** 用例目的：分别尝试修改选择 Map 和得分 Map，要求抛 UnsupportedOperationException，随后确认总分/明细保持一致。 */
    @Test void gradingResultsCannotBeChangedAfterCalculation() {
        GradingService.GradeResult result = grading.grade(items(),
                Arrays.asList(answer(1L, "A"), answer(2L, "B")));
        // final 只能防止引用被替换；集合本身也要拒绝修改，保证总分与明细始终一致。
        assertThatThrownBy(() -> result.selected.put(1L, "D"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.points.put(1L, 0))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(result.score).isEqualTo(30);
        assertThat(result.selected).containsEntry(1L, "A");
        assertThat(result.points).containsEntry(1L, 10);
    }
    /** 用例目的：同一 itemId 提交两次必须抛 BusinessException，不能静默保留最后一个答案。 */
    @Test void duplicateAnswersAreRejected() {
        assertThatThrownBy(() -> grading.grade(items(), Arrays.asList(answer(1L, "A"), answer(1L, "B"))))
                .isInstanceOf(BusinessException.class);
    }
    /** 用例目的：提交不存在于这张卷的明细 ID，判分组件独立拒绝，而不依赖 HTTP 层。 */
    @Test void otherAttemptItemIsRejected() {
        assertThatThrownBy(() -> grading.grade(items(), Collections.singletonList(answer(9L, "A"))))
                .isInstanceOf(BusinessException.class);
    }
    /** 用例目的：提交选项 E，要求判分业务拒绝，不仅依赖 DTO 正则校验。 */
    @Test void invalidOptionIsRejected() {
        assertThatThrownBy(() -> grading.grade(items(), Collections.singletonList(answer(1L, "E"))))
                .isInstanceOf(BusinessException.class);
    }
    /** 用例目的：列表含 null 元素时抛业务异常，避免运行到空指针异常。 */
    @Test void nullAnswerIsRejected() {
        assertThatThrownBy(() -> grading.grade(items(), Collections.singletonList(null)))
                .isInstanceOf(BusinessException.class);
    }
    /** 用例目的：验证第 2 页偏移为 10；第 0 页、int 最大页码和每页 51 条均被边界检查拒绝。 */
    @Test void paginationLimitsPreventOverflow() {
        assertThat(new PageQuery(2, 10).offset).isEqualTo(10);
        assertThatThrownBy(() -> new PageQuery(0, 10)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new PageQuery(Integer.MAX_VALUE, 50)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new PageQuery(1, 51)).isInstanceOf(BusinessException.class);
    }
    /** 构造两条脱离数据库的快照对象：ID 1/A/10 分与 ID 2/B/20 分，隔离判分算法对持久层的依赖。 */
    private List<AttemptItem> items() {
        AttemptItem one = new AttemptItem();
        one.setId(1L); one.setCorrectOptionSnapshot("A"); one.setPointsSnapshot(10);
        AttemptItem two = new AttemptItem();
        two.setId(2L); two.setCorrectOptionSnapshot("B"); two.setPointsSnapshot(20);
        return Arrays.asList(one, two);
    }
    /** 构造一个学生答案 DTO，供全对、全错、重复或非法输入等单元用例复用。 */
    private Answer answer(Long id, String selected) {
        Answer answer = new Answer();
        answer.setItemId(id); answer.setSelectedOption(selected);
        return answer;
    }
}
