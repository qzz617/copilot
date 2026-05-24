package com.cmbchina.cs.assitsvc.push;

import com.alibaba.fastjson2.JSON;
import com.cmbchina.cs.assitsvc.domain.DirectiveDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 接收 Redis Pub/Sub 集群推送消息，并投递给本 Pod 的 WebSocket broker。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CopilotPushRedisSubscriber implements MessageListener {

    private final CopilotPushService pushService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null || message.getBody() == null) {
            return;
        }
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            DirectiveDTO directive = JSON.parseObject(payload, DirectiveDTO.class);
            pushService.pushLocalDirective(directive);
        } catch (Exception e) {
            log.warn("[M10] Parse cluster directive message failed, payload={}", payload, e);
        }
    }
}
