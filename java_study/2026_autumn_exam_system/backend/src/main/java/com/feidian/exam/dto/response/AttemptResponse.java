/* 文件导读：学生答卷响应：把 Attempt 与不含正确答案的 StudentItemResponse 组合成 JSON。进行中可看试题，已交卷由 resultUrl 指引查成绩。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.dto.response;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.model.Attempt;
import java.util.List;

public class AttemptResponse {
    /** courseId：所属课程 course.id。 */
    public final Long attemptId, courseId;
    /** resultUrl：已交卷时的成绩查询相对路径，进行中为 null。 */
    public final String courseName, status, resultUrl;
    /** totalScore：开考时整张试卷的满分，不固定为 100。 */
    public final Integer attemptNo, totalScore;
    /** items：本次试卷的学生可见题目列表。 */
    public final List<StudentItemResponse> items;

    /** 构造当前响应并显式复制允许输出的字段；不执行数据库查询或判分。 */
    public AttemptResponse(Attempt attempt, List<StudentItemResponse> items) {
        this.attemptId = attempt.getId();
        this.courseId = attempt.getCourseId();
        this.courseName = attempt.getCourseName();
        this.status = attempt.getStatus();
        this.attemptNo = attempt.getAttemptNo();
        this.totalScore = attempt.getTotalScore();
        this.items = items;
        this.resultUrl = "SUBMITTED".equals(status) ? "/api/student/attempts/" + attemptId + "/result" : null;
    }
}
