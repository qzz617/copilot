package com.cmbchina.cs.assitsvc.push;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Copilot WebSocket 安全配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "copilot.websocket")
public class WebSocketProperties {

    /** 允许连接 WebSocket 的前端 Origin。为空时使用 Spring 默认同源策略。 */
    private List<String> allowedOriginPatterns = new ArrayList<>();
}
