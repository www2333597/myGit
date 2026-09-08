/* 文件导读：可预期业务失败载体：HTTP 状态 + 稳定业务 code + 可展示 message。继承 RuntimeException，能被 Spring MVC 统一处理，也能沿调用链触发 Service 事务回滚。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.common;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    /** HTTP 状态或答卷状态，具体以本类声明类型为准。 */
    public final HttpStatus status;
    /** 稳定的业务状态码，客户端据此判断失败原因。 */
    public final String code;

    /** 把 message 交给异常父类，并保存 HTTP 状态及业务码；这里只构造异常，调用处 throw 才中断流程。 */
    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /** 输入值或业务数据不合法时构造 400/INVALID_INPUT；参数格式错误与权限不足应分开。 */
    public static BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message);
    }

    /** 已知身份却不允许操作时构造 403/FORBIDDEN，例如教师访问别人的课程。 */
    public static BusinessException forbidden() {
        return new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "没有访问权限");
    }

    /** 资源不存在时构造 404/NOT_FOUND；他人答卷也用同一响应，减少编号探测。 */
    public static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "资源不存在");
    }

    /** 请求格式合法但当前状态不允许时构造 409，例如空题库、需要重考或尚未交卷。 */
    public static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
