package com.cmbchina.cs.assitsvc.infra.redis;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Jedis 连接池配置。
 * 显式声明 JedisPool bean，供需要 Pipeline 的组件直接注入。
 */
@Configuration
@EnableConfigurationProperties(HistoryProperties.class)
public class RedisConfig {

    @Bean
    public JedisPool jedisPool(RedisProperties redisProperties) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        RedisProperties.Pool pool = redisProperties.getJedis().getPool();
        if (pool != null) {
            poolConfig.setMaxTotal(pool.getMaxActive());
            poolConfig.setMaxIdle(pool.getMaxIdle());
            poolConfig.setMinIdle(pool.getMinIdle());
            if (pool.getMaxWait() != null) {
                poolConfig.setMaxWaitMillis(pool.getMaxWait().toMillis());
            }
        }
        String password = StringUtils.hasText(redisProperties.getPassword())
                ? redisProperties.getPassword() : null;
        return new JedisPool(
                poolConfig,
                redisProperties.getHost(),
                redisProperties.getPort(),
                (int) redisProperties.getTimeout().toMillis(),
                password,
                redisProperties.getDatabase()
        );
    }
}
