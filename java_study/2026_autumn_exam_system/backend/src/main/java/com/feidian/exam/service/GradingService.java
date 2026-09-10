/* 文件导读：可独立测试的 Spring 判分组件。Set 验证题目归属，Map 按明细 ID 关联选项；不访问 MySQL。先验证全部答案，再计算整卷，结果集合经过复制与只读包装。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.service;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.common.BusinessException;
import com.feidian.exam.dto.request.SubmitAttemptRequest.Answer;
import com.feidian.exam.model.AttemptItem;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 纯判分组件：输入试题快照和答案，输出分数，不访问数据库。
 * 因而可用普通单元测试直接 new 出来测试，问题也更容易定位。
 */
@Service
public class GradingService {
    /** 本次交卷最多接收 1000 个答案，与 DTO/开考上限一致。 */
    private static final int MAX_ANSWERS = 1000;

    /** 先完整校验，再计算分数；调用方拿到结果之后，才能进入数据库保存阶段。 */
    /** 判分公开入口：先 validateAndIndexAnswers 获得合法 ID/选项映射，再 calculateScore 产生总分与逐题得分。 */
    public GradeResult grade(List<AttemptItem> items, List<Answer> answers) {
        Map<Long, String> selected = validateAndIndexAnswers(items, answers);
        return calculateScore(items, selected);
    }

    /** 将答案整理为“试题明细 ID -> 选项”，请求中答案的排列顺序不影响判分。 */
    /** 检查列表大小、对象/编号非空、属于本卷、选项 A–D、无重复 ID；任何错误立即抛异常，不能将同题后一答案覆盖前一答案。 */
    private Map<Long, String> validateAndIndexAnswers(List<AttemptItem> items, List<Answer> answers) {
        if (answers == null || answers.size() > MAX_ANSWERS) {
            throw BusinessException.badRequest("答案列表不正确");
        }
        Set<Long> validIds = new HashSet<>();
        for (AttemptItem item : items) {
            validIds.add(item.getId());
        }
        Map<Long, String> selected = new HashMap<>();
        for (Answer answer : answers) {
            // 先检查对象和编号，再访问其他字段，避免空指针；每个分支只解释一种失败原因。
            if (answer == null || answer.getItemId() == null) {
                throw BusinessException.badRequest("答案中的题目编号不能为空");
            }
            Long itemId = answer.getItemId();
            String option = answer.getSelectedOption();
            // DTO 只能校验输入格式，题目是否属于这张答卷还需要业务层检查。
            if (!validIds.contains(itemId)) {
                throw BusinessException.badRequest("答案中包含不属于本答卷的题目");
            }
            if (option == null || !option.matches("[ABCD]")) {
                throw BusinessException.badRequest("选项只能是 A、B、C 或 D");
            }
            if (selected.containsKey(itemId)) {
                throw BusinessException.badRequest("同一道题不能重复提交");
            }
            selected.put(itemId, option);
        }
        return selected;
    }

    /** 遍历试卷而非答案列表，确保漏答的题目也生成一条 0 分明细。 */
    /** 遍历全部快照题，用学生选项比较快照正确答案，错答/漏答得 0，累加得分并保留每题结果。 */
    private GradeResult calculateScore(List<AttemptItem> items, Map<Long, String> selected) {
        int totalScore = 0;
        Map<Long, Integer> earnedPoints = new LinkedHashMap<>();
        for (AttemptItem item : items) {
            String option = selected.get(item.getId());
            // 漏答时 option 为 null，equals 返回 false，因此自然记 0 分。
            int earned = item.getCorrectOptionSnapshot().equals(option) ? item.getPointsSnapshot() : 0;
            earnedPoints.put(item.getId(), earned);
            // addExact 遇到整数溢出会抛异常，使外层事务回滚，而不是保存一个错误的负分。
            totalScore = Math.addExact(totalScore, earned);
        }
        return new GradeResult(totalScore, selected, earnedPoints);
    }

    /** 只读结果：已经算好的总分、选择和每题得分不能被调用方改得相互矛盾。 */
    public static final class GradeResult {
        /** 这次判分得到的总分。 */
        public final int score;
        /** 按试题明细 ID 保存的学生选项只读映射。 */
        public final Map<Long, String> selected;
        /** 按试题明细 ID 保存的所得分只读映射。 */
        public final Map<Long, Integer> points;

        /** 复制并冻结结果集合；保留数值内容和题目顺序，阻止外部代码造成总分/明细不一致。 */
        GradeResult(int score, Map<Long, String> selected, Map<Long, Integer> points) {
            this.score = score;
            // final 防止换引用，复制隔离原集合，只读包装拒绝 put/remove/clear 等修改。
            // Long、String、Integer 也都是不可变类型，所以这里不需要递归复制元素。
            this.selected = Collections.unmodifiableMap(new HashMap<>(selected));
            this.points = Collections.unmodifiableMap(new LinkedHashMap<>(points));
        }
    }
}
