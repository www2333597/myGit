/* 文件导读：Spring MVC 的集中异常出口。RestControllerAdvice = 面向 Controller 的增强 + JSON 响应；ExceptionHandler 按异常类型选择处理方法，不是每个方法都执行。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.common;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

/**
 * 全局异常转换：业务异常保留业务码，输入错误统一成 400，未知异常只在服务端记类型。
 * 这样 Controller 不需要重复 try/catch，也不会把 SQL、堆栈或密码回显给调用方。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /** SLF4J 日志对象，只记录必要诊断信息，不输出密码。 */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    /** 将已知业务异常转换为对应 HTTP 状态和 ApiResponse；稳定 code 用于客户端分支判断，message 供人阅读。 */
    public ResponseEntity<ApiResponse<Void>> business(BusinessException ex) {
        // BusinessException 已包含预期的 HTTP 状态和稳定业务 code。
        return ResponseEntity.status(ex.status).body(ApiResponse.error(ex.code, ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    /** 处理 JSON 解析失败、类型转换失败、Bean Validation 失败及缺少参数，统一 400 且不回显原始异常内容。 */
    public ResponseEntity<ApiResponse<Void>> invalid(Exception ex) {
        // 不回显请求中的密码、SQL 或内部异常内容。
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_INPUT", "请求字段、格式或取值不正确"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    /** MVC 无匹配处理器时返回 404；还依赖 application.yml 的 throw-exception-if-no-handler-found 配置。 */
    public ResponseEntity<ApiResponse<Void>> missing(NoHandlerFoundException ex) {
        return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "接口不存在"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    /** 路径存在但 HTTP 动作不支持时返回 405，例如把只读 GET 接口当 POST 调用。 */
    public ResponseEntity<ApiResponse<Void>> method(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(405).body(ApiResponse.error("METHOD_NOT_ALLOWED", "请求方法不支持"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    /** 请求体媒体类型不受支持时返回 415；JSON 接口需要正确 Content-Type。 */
    public ResponseEntity<ApiResponse<Void>> media(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(415).body(ApiResponse.error("UNSUPPORTED_MEDIA_TYPE", "请使用 application/json"));
    }

    @ExceptionHandler(Exception.class)
    /** 兜底处理其他 Exception；服务端仅记录异常类名，对外 500，不把 SQL 或堆栈暴露给客户端。 */
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception ex) {
        // 不把 ex.getMessage() 发给客户端；线上还应给日志增加请求 ID 并接入审计/监控。
        log.error("Unhandled request failure ({})", ex.getClass().getSimpleName());
        return ResponseEntity.status(500).body(ApiResponse.error("INTERNAL_ERROR", "服务暂时不可用，请稍后重试"));
    }
}
