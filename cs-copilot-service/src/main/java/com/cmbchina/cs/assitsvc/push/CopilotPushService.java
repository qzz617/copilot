package com.cmbchina.cs.assitsvc.push;

import com.alibaba.fastjson2.JSON;
import com.cmbchina.cs.assitsvc.domain.DirectiveDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Copilot WebSocket 推送服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopilotPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;
    private final WebSocketProperties properties;

    /**
     * 推送推荐指令。
     *
     * @param directive 推荐指令
     * @return true 表示推送调用成功
     */
    public boolean pushDirective(DirectiveDTO directive) {
        if (directive == null || !StringUtils.hasText(directive.getOperatorId())) {
            throw new IllegalArgumentException("directive and operatorId must not be empty");
        }

        try {
            redisTemplate.convertAndSend(properties.getClusterPushChannel(), JSON.toJSONString(directive));
            log.info("[M10] Directive published, directiveId={}, operatorId={}",
                    directive.getDirectiveId(), directive.getOperatorId());
            return true;
        } catch (Exception e) {
            log.error("[M10] Directive publish failed, directiveId={}, operatorId={}",
                    directive.getDirectiveId(), directive.getOperatorId(), e);
            return false;
        }
    }

    /**
     * 将 Redis Pub/Sub 收到的指令推送给本 Pod 上的 WebSocket 连接。
     */
    public void pushLocalDirective(DirectiveDTO directive) {
        if (directive == null || !StringUtils.hasText(directive.getOperatorId())) {
            return;
        }

        messagingTemplate.convertAndSendToUser(directive.getOperatorId(), "/copilot/directive", directive);
        log.info("[M10] Directive pushed locally, directiveId={}, operatorId={}",
                directive.getDirectiveId(), directive.getOperatorId());
    }
}
