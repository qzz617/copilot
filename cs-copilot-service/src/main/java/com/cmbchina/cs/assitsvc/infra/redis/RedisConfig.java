package com.cmbchina.cs.assitsvc.infra.redis;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 业务配置。
 *
 * <p>连接工厂、StringRedisTemplate 和 RedisTemplate 由 Spring Boot 自动配置创建，
 * 生产可通过 spring.redis.* 配置切换单节点、Sentinel 或 Cluster 拓扑。
 */
@Configuration
@EnableConfigurationProperties(HistoryProperties.class)
public class RedisConfig {
}
