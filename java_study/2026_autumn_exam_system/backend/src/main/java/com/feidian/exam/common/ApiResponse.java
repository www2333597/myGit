/* 文件导读：Spring MVC 统一返回结构；Jackson 序列化这个普通 Java 对象。T 表示任意业务数据类型，HTTP 状态由 Controller/异常处理器另外设置。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.common;

/** 统一 JSON 外壳：成功和失败都有 code/message/data，前端或 Apifox 不必猜响应结构。 */
public class ApiResponse<T> {
    /** 稳定的业务状态码，客户端据此判断失败原因。 */
    public final String code;
    /** 给人阅读的响应/异常说明。 */
    public final String message;
    /** 泛型业务数据，失败或无数据时为 null。 */
    public final T data;

    /** 私有构造器集中设置业务码、提示、数据；外部通过静态工厂保证成功/失败结构一致。 */
    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 泛型 T 使用户、课程、分页结果都能复用外壳，同时保留编译期类型检查。
    /** 创建业务成功结果，data 的具体类型由泛型 T 推断；这一步本身不设置 HTTP 状态码。 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "成功", data);
    }

    /** 创建失败结果，data 固定 null；Void 表示没有业务数据，不要把失败仍伪装成成功分数。 */
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
