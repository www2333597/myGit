/* 文件导读：用 springdoc 生成 OpenAPI 文档；这里描述接口而不执行鉴权。真正保护请求的是 AuthInterceptor 和 Service。离线导入 Apifox 后还需设置运行环境与 Cookie。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.config;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.*;
import java.math.BigDecimal;
import java.util.Collections;

@Configuration
public class OpenApiConfig {
    @Bean
    /** 设置 API 标题/版本、根相对服务地址和两种安全方案；同一个 SecurityRequirement 中的 Cookie 与 CSRF 表示同时满足。 */
    public OpenAPI examOpenApi() {
        return new OpenAPI().info(new Info().title("在线考试系统 API").version("0.1.0")
                .description("先获取 CSRF Token，再登录。写请求携带 X-CSRF-Token；会话使用 JSESSIONID Cookie。"))
                // 当前应用部署在根路径；相对地址跟随文档的主机和端口，换端口后仍能调试。
                .servers(Collections.singletonList(new Server().url("/")
                        .description("跟随当前文档的主机和端口；导入 Apifox 后设置实际环境地址")))
                .components(new Components()
                        .addSecuritySchemes("sessionCookie", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE).name("JSESSIONID"))
                        .addSecuritySchemes("csrfToken", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name("X-CSRF-Token")));
    }

    @Bean
    /** 返回启动后用于定制文档的 lambda：补错误模型、逐路径设置安全要求、写请求 Token 头、创建 201 和分页范围；它不是 Controller。 */
    public OpenApiCustomiser contractDetails() {
        return api -> {
            // components/schemas 定义可复用错误结构，后面的响应通过 $ref 引用，避免重复展开。
            api.getComponents().addSchemas("ErrorResponse", new ObjectSchema()
                    .addProperty("code", new StringSchema()).addProperty("message", new StringSchema())
                    .addProperty("data", new Schema<>().nullable(true)));
            // 外层遍历路径，内层遍历同一路径的 GET/POST 等操作；一个路径可能对应多个接口。
            api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                boolean anonymous = path.equals("/api/health") || path.equals("/api/auth/csrf")
                        || path.equals("/api/auth/login");
                boolean write = method == PathItem.HttpMethod.POST || method == PathItem.HttpMethod.PUT
                        || method == PathItem.HttpMethod.DELETE;
                // 单个 requirement 内添加多个方案是 AND；放入多个 requirement 对象则是 OR。
                SecurityRequirement security = new SecurityRequirement();
                if (!anonymous) security.addList("sessionCookie");
                if (write) {
                    // 声明写请求需要 Token，并把实际请求头展示在调试页；不会在此生成或验证 Token。
                    security.addList("csrfToken");
                    operation.addParametersItem(new HeaderParameter().name("X-CSRF-Token").required(true)
                            .description("当前会话令牌；登录成功后须使用响应中的新令牌")
                            .schema(new StringSchema()));
                }
                // 公共 GET 不要求凭据；业务文档与运行时拦截器逻辑需要同步维护。
                operation.setSecurity(security.isEmpty() ? Collections.emptyList() : Collections.singletonList(security));
                if (method == PathItem.HttpMethod.POST && (path.endsWith("/questions") || path.endsWith("/attempts"))) {
                    // 新建题目只用 201；开考还可能复用旧卷，所以保留 200 并增加 201。
                    ApiResponse success = operation.getResponses().get("200");
                    operation.getResponses().addApiResponse("201", new ApiResponse().description("创建成功")
                            .content(success == null ? null : success.getContent()));
                    if (path.endsWith("/questions")) operation.getResponses().remove("200");
                }
                for (String status : new String[]{"400", "401", "403", "404", "409", "500"}) {
                    // 给常见失败状态补同一错误模型，具体业务 code 由运行时异常决定。
                    if (anonymous && status.equals("401") && !path.endsWith("/login")) continue;
                    operation.getResponses().addApiResponse(status, new ApiResponse().description("参见响应中的业务 code")
                            .content(new Content().addMediaType("application/json", new MediaType()
                                    .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")))));
                }
                if (operation.getParameters() != null) operation.getParameters().forEach(parameter -> {
                    // 文档范围与 PageQuery 对齐；BigDecimal 是 OpenAPI 模型的数值边界类型。
                    if ("page".equals(parameter.getName())) parameter.setSchema(new IntegerSchema()
                            .minimum(BigDecimal.ONE).maximum(BigDecimal.valueOf(100000))._default(1));
                    if ("pageSize".equals(parameter.getName())) parameter.setSchema(new IntegerSchema()
                            .minimum(BigDecimal.ONE).maximum(BigDecimal.valueOf(50))._default(10));
                });
            }));
        };
    }
}
