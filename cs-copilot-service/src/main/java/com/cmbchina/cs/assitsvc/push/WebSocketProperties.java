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

    /** 集群推送使用的 Redis Pub/Sub channel。 */
    private String clusterPushChannel = "copilot:push:directive";

    /** WebSocket 握手时读取坐席工号的请求头。 */
    private String operatorIdHeader = "X-Copilot-Operator-Id";

    /** WebSocket 握手时读取坐席工号的 query 参数。 */
    private String operatorIdQueryParam = "operatorId";
}
