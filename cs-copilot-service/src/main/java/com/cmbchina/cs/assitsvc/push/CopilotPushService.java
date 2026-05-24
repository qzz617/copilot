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
     * 真实送达状态需要前端 ACK 反馈接口确认（本期未实现）。
     *
     * <p><b>失败语义</b>：参数非法、Redis 故障等所有失败场景都统一返回 false（仅记日志），
     * 不再抛出 IllegalArgumentException。调用方仅需通过 boolean 返回值判定成败。
     *
     * @param directive 推荐指令
     * @return true 表示 Redis publish 调用成功；false 表示参数非法或 publish 异常
     */
    public boolean publishDirectiveAsync(DirectiveDTO directive) {
        if (directive == null || !StringUtils.hasText(directive.getOperatorId())) {
            log.warn("[M10] Invalid directive, skip publish, directive={}", directive);
            return false;
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
