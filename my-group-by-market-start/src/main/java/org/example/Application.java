package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 启动类
 *
 * <p>
 * 关键点：
 * <ul>
 * <li>1. 包名必须是 org.example (父包)</li>
 * <li>这样它能自动扫描到：
 * <ul>
 * <li>org.example.application (Service)</li>
 * <li>org.example.interfaces (Controller)</li>
 * <li>org.example.infrastructure (RepositoryImpl, Config)</li>
 * </ul>
 * </li>
 * <li>2. @SpringBootApplication
 * 包含了 @ComponentScan, @EnableAutoConfiguration, @Configuration</li>
 * <li>3. @EnableScheduling 启用定时任务调度</li>
 * <li>4. @EnableAsync 启用异步方法支持（用于事件驱动settlement）</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  项目启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}