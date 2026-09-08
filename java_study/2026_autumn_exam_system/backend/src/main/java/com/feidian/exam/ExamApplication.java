/* 文件导读：Spring Boot 启动入口。SpringBootApplication 组合配置类、组件扫描和自动配置；MapperScan 把 MyBatis 接口注册为可注入代理。业务 Bean 默认单例，因此不要在 Service 字段保存某个学生的请求数据。 */
// package 划分命名空间与职责；Spring 的组件扫描范围从启动类所在包开始。
package com.feidian.exam;

// import 引入下面使用的类型/静态方法名，不会在这里创建对象、连接数据库或执行请求。
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 程序入口：仍然是熟悉的 main 方法，Spring Boot 帮我们启动 Web 服务并组装对象。
 * Spring 管对象（IoC）和依赖注入（DI），Spring MVC 处理 HTTP，MyBatis 执行 SQL。
 * 本项目不是把业务交给框架自动生成；考试规则仍由 service 包里的 Java 方法决定。
 */
@SpringBootApplication // 自动配置，并从 com.feidian.exam 开始扫描带组件注解的类。
// 为这些 Mapper 接口创建代理对象；实际 SQL 位于 resources/mapper 中的同名 XML。
@MapperScan("com.feidian.exam.mapper")
public class ExamApplication {
    /** Java 入口调用 SpringApplication.run：准备环境、创建 IoC 容器、装配 Bean 并启动内嵌 Tomcat；main 线程不是某个学生的请求处理方法。 */
    public static void main(String[] args) {
        SpringApplication.run(ExamApplication.class, args);
    }
}
