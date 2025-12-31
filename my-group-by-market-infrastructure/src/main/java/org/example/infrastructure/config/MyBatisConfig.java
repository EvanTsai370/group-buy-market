package org.example.infrastructure.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// 【关键】只扫描 Infrastructure 层里的 Mapper 包
// 不要扫描到 Domain 或其他层去了
@MapperScan("org.example.infrastructure.persistence.mapper")
public class MyBatisConfig {

    /**
     * MyBatis-Plus 插件配置
     * 乐观锁拦截器：用于 Order 聚合的并发控制
     *
     * 使用场景：
     * - Order: 多个用户可能同时加入同一个拼团订单，需要乐观锁防止超卖
     *
     * 要求：
     * - PO 类中需要有 @Version 注解的字段（目前只有 OrderPO）
     * - updateById() 时自动添加 WHERE version = ? 条件，并递增版本号
     * - 如果版本号不匹配（并发冲突），updateById() 返回 0
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }
}