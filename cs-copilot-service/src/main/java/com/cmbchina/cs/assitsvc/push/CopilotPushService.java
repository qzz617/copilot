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
     * 异步发布推荐指令到集群推送通道（Redis Pub/Sub）。
     *
     * <p><b>语义</b>：返回 true 仅表示 Redis publish 调用成功，不保证消息已送达任何 WebSocket 连接。
     * 真实送达状态需要前端 ACK 反馈接口确认（本期未实现，预留方法 markDirectiveDelivered）。
     *
     * @param directive 推荐指令
     * @return true 表示 Redis publish 调用成功
     */
    public boolean publishDirectiveAsync(DirectiveDTO directive) {
        if (directive == null || !StringUtils.hasText(directive.getOperatorId())) {
            throw new IllegalArgumentException("directive and operatorId must not be empty");
        }

        try {
            redisTemplate.convertAndSend(properties.getClusterPushChannel(), JSON.toJSONString(directive));
            log.info("[M10] Directive published to cluster channel, directiveId={}, operatorId={}",
                    directive.getDirectiveId(), directive.getOperatorId());
            return true;
        } catch (Exception e) {
            log.error("[M10] Directive publish failed, directiveId={}, operatorId={}",
                    directive.getDirectiveId(), directive.getOperatorId(), e);
            return false;
        }
    }

    /**
     * 标记指令已送达前端（前端 ACK 反馈调用）。
     *
     * <p><b>本期未实现</b>。当前 directive_status 只有 PUBLISHED 状态。
     * 未来前端 SDK 增加 ACK 反馈后，由反馈接口调用本方法把 trigger_log 状态推进到 DELIVERED。
     *
     * @param directiveId 指令 ID
     */
    public void markDirectiveDelivered(String directiveId) {
        if (!StringUtils.hasText(directiveId)) {
            throw new IllegalArgumentException("directiveId must not be null or empty");
        }
        log.debug("[M10] markDirectiveDelivered placeholder, directiveId={}", directiveId);
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
