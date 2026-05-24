package com.cmbchina.cs.assitsvc.core.event;

import com.cmbchina.cs.assitsvc.domain.DirectiveDTO;
import com.cmbchina.cs.assitsvc.push.CopilotPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 指令推送监听器：将构建好的指令通过 WebSocket 集群通道推送给坐席。
 *
 * <p>推送失败时仅打 warn 日志，不再次发布事件（避免事件链导致调试复杂）。
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class DirectivePushListener {

    private final CopilotPushService pushService;

    /**
     * 监听指令构建成功事件，触发实际推送动作。
     *
     * @param event 指令构建事件
     */
    @EventListener
    public void onDirectivePrepared(DirectivePreparedEvent event) {
        DirectiveDTO directive = event.getDirective();
        try {
            boolean published = pushService.publishDirectiveAsync(directive);
            if (!published) {
                log.warn("[M10] Directive push failed, directiveId={}, callId={}, operatorId={}",
                        directive.getDirectiveId(), directive.getCallId(), directive.getOperatorId());
            }
        } catch (Exception e) {
            // 任何异常都吞掉，避免影响其他监听器（日志、ES 等）
            log.warn("[M10] Directive push threw exception, directiveId={}, callId={}",
                    directive.getDirectiveId(), directive.getCallId(), e);
        }
    }
}
