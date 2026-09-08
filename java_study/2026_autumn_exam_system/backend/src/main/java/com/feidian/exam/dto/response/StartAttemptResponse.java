/* 文件导读：开考结果：只返回答卷编号、次数、状态及 reused。Controller 用 reused 决定 HTTP 200（复用）或 201（新建），详细试题另行查询。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.dto.response;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.model.Attempt;

public class StartAttemptResponse {
    /** attemptId：本次考试 exam_attempt.id。 */
    public final Long attemptId;
    /** attemptNo：该学生该课程的考试次数序号，从 1 开始。 */
    public final Integer attemptNo;
    /** status：答卷状态 IN_PROGRESS/SUBMITTED。 */
    public final String status;
    /** reused：是否复用进行中答卷；不是本次考试是否属于重考。 */
    public final boolean reused;

    /** 构造当前响应并显式复制允许输出的字段；不执行数据库查询或判分。 */
    public StartAttemptResponse(Attempt attempt, boolean reused) {
        this.attemptId = attempt.getId();
        this.attemptNo = attempt.getAttemptNo();
        this.status = attempt.getStatus();
        this.reused = reused;
    }
}
