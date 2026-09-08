/* 文件导读：学生看题响应白名单：只包含试题明细 ID、题序、题干、四个选项和分值。标准答案保留在服务端 MySQL 快照中，不能发送给学生。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.dto.response;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.model.AttemptItem;

/** 学生看卷专用 DTO，特意没有 correctOptionSnapshot 与 earnedPoints 字段。 */
public class StudentItemResponse {
    /** itemId：本张答卷的 exam_attempt_item.id；不是 question.id。 */
    public final Long itemId;
    /** points：单题正整数分值（1–100）。 */
    public final Integer positionNo, points;
    /** optionD：D 选项文本。 */
    public final String stem, optionA, optionB, optionC, optionD;

    /** 构造当前响应并显式复制允许输出的字段；不执行数据库查询或判分。 */
    public StudentItemResponse(AttemptItem item) {
        // 白名单式复制响应字段，比直接序列化 AttemptItem 更不容易意外泄露标准答案。
        this.itemId = item.getId();
        this.positionNo = item.getPositionNo();
        this.points = item.getPointsSnapshot();
        this.stem = item.getStemSnapshot();
        this.optionA = item.getOptionASnapshot();
        this.optionB = item.getOptionBSnapshot();
        this.optionC = item.getOptionCSnapshot();
        this.optionD = item.getOptionDSnapshot();
    }
}
