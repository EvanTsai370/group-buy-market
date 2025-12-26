package org.example.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
// 【关键】只扫描 Infrastructure 层里的 Mapper 包
// 不要扫描到 Domain 或其他层去了
@MapperScan("org.example.infrastructure.persistence.mapper")
public class MyBatisConfig {
    // 这里以后还可以配置分页插件等
}