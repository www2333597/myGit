/* 文件导读：开考请求 DTO：FIRST 是首次考试，RETAKE 是重考；这里只校验模式格式，有无历史和进行中答卷由 ExamService 查询 MySQL 后判断。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.dto.request;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import javax.validation.constraints.*;

public class StartAttemptRequest {
    // @NotNull 要求模式存在；正则中的 | 表示二选一，只接收完整 FIRST 或 RETAKE，不接收小写。
    @NotNull @Pattern(regexp = "FIRST|RETAKE")
    /** mode：开考模式 FIRST/RETAKE。 */
    private String mode;

    /** 读取开考模式 FIRST/RETAKE；供 Service 取出已绑定的请求值。 */
    public String getMode() { return mode; }
    /** 设置开考模式 FIRST/RETAKE；由 Jackson 绑定请求或由业务层规范化，此方法本身不执行 SQL。 */
    public void setMode(String mode) { this.mode = mode; }
}
