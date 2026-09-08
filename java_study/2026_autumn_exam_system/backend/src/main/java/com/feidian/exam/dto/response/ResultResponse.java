/* 文件导读：交卷/查分共用响应：来自已经保存的 Attempt；得分与卷面满分分开，时间使用明确 JSON 格式。重复交卷复用原结果而不重新判分。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.dto.response;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.model.Attempt;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class ResultResponse {
    /** courseId：所属课程 course.id。 */
    public final Long attemptId, courseId;
    /** totalScore：开考时整张试卷的满分，不固定为 100。 */
    public final Integer attemptNo, score, totalScore;
    /** status：答卷状态 IN_PROGRESS/SUBMITTED。 */
    public final String courseName, status;
    // @JsonFormat 控制 LocalDateTime 的 JSON 字符串格式；类型自身不保存时区信息。
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** startedAt：服务端记录的开考时间，本项目按北京时间处理。 */
    public final LocalDateTime startedAt;
    // @JsonFormat 控制 LocalDateTime 的 JSON 字符串格式；类型自身不保存时区信息。
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** submittedAt：第一次成功交卷的时间，重复提交不更新它。 */
    public final LocalDateTime submittedAt;

    /** 构造当前响应并显式复制允许输出的字段；不执行数据库查询或判分。 */
    public ResultResponse(Attempt attempt) {
        this.attemptId = attempt.getId();
        this.courseId = attempt.getCourseId();
        this.attemptNo = attempt.getAttemptNo();
        this.score = attempt.getScore();
        this.totalScore = attempt.getTotalScore();
        this.courseName = attempt.getCourseName();
        this.status = attempt.getStatus();
        this.startedAt = attempt.getStartedAt();
        this.submittedAt = attempt.getSubmittedAt();
    }
}
