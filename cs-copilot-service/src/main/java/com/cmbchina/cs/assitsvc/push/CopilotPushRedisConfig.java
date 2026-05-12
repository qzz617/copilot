package com.cmbchina.cs.assitsvc.push;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * WebSocket 集群推送 Redis Pub/Sub 配置。
 */
@Configuration
@RequiredArgsConstructor
public class CopilotPushRedisConfig {

    private final WebSocketProperties properties;

    @Bean
    public RedisMessageListenerContainer copilotPushRedisListenerContainer(
            RedisConnectionFactory connectionFactory,
            CopilotPushRedisSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(properties.getClusterPushChannel()));
        return container;
    }
}
