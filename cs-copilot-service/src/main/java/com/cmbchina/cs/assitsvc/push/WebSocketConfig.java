package com.cmbchina.cs.assitsvc.push;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Copilot WebSocket 推送基础配置。
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties properties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        if (CollectionUtils.isEmpty(properties.getAllowedOriginPatterns())) {
            registry.addEndpoint("/copilot/ws").withSockJS();
            return;
        }
        registry.addEndpoint("/copilot/ws")
                .setAllowedOriginPatterns(properties.getAllowedOriginPatterns().toArray(new String[0]))
                .withSockJS();
    }
}
