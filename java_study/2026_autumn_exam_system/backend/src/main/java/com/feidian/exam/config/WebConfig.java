/* 文件导读：Spring 配置类，使用 WebMvcConfigurer 扩展 Boot 默认 MVC 配置。没有使用 EnableWebMvc 全面接管 MVC；BCrypt 工具通过 Bean 方法提供，拦截器通过注册表加入请求链路。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam.config;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import com.feidian.exam.security.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.*;
import java.util.Arrays;

/** 配置对象也是 Spring 管理的 Bean；这里集中注册拦截器、跨域规则和密码校验工具。 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    /** 认证业务/拦截器依赖，由 Spring 注入。 */
    private final AuthInterceptor auth;
    private final String[] allowedOrigins;

    /** 构造器注入 AuthInterceptor，@Value 读取来源配置；Stream 分割逗号、去首尾空格、过滤空值并形成精确允许名单。 */
    public WebConfig(AuthInterceptor auth, @Value("${exam.cors.allowed-origins}") String origins) {
        this.auth = auth;
        this.allowedOrigins = Arrays.stream(origins.split(",")).map(String::trim)
                .filter(value -> !value.isEmpty()).toArray(String[]::new);
    }

    @Override
    /** 注册 AuthInterceptor 到 /api/**；注册决定拦截范围，Component 注解本身不会自动把拦截器挂到请求链上。 */
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auth).addPathPatterns("/api/**");
    }

    @Override
    /** 为 API 配置允许源、方法、请求头和 Cookie 凭据；600 秒是预检缓存时长，不是 Session 有效期。 */
    public void addCorsMappings(CorsRegistry registry) {
        // 带 Cookie 的跨域请求使用精确来源名单。CORS 是浏览器规则，不是账号权限系统。
        registry.addMapping("/api/**").allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "X-CSRF-Token")
                .allowCredentials(true).maxAge(600);
    }

    @Bean
    /** 声明 BCryptPasswordEncoder Bean；强度 10 是成本参数而不是简单循环 10 次，AuthService 依赖接口 PasswordEncoder 使用它。 */
    public PasswordEncoder passwordEncoder() {
        // @Bean 把第三方类的实例交给容器，AuthService 通过 PasswordEncoder 接口接收它。
        // 这里只引入 security-crypto；项目没有使用完整 Spring Security 过滤器链。
        return new BCryptPasswordEncoder(10);
    }
}
