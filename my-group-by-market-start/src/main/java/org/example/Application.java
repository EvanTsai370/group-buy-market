package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类
 * * 关键点：
 * 1. 包名必须是 org.example (父包)
 * 这样它能自动扫描到：
 * - org.example.application (Service)
 * - org.example.interfaces (Controller)
 * - org.example.infrastructure (RepositoryImpl, Config)
 * * 2. @SpringBootApplication
 * 包含了 @ComponentScan, @EnableAutoConfiguration, @Configuration
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  项目启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}