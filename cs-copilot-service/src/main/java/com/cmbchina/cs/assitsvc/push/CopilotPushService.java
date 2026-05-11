package com.cmbchina.cs.assitsvc.push;

import com.cmbchina.cs.assitsvc.domain.DirectiveDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        String destination = "/user/" + directive.getOperatorId() + "/copilot/directive";
        try {
            messagingTemplate.convertAndSend(destination, directive);
            log.info("[M10] Directive pushed, directiveId={}, operatorId={}",
                    directive.getDirectiveId(), directive.getOperatorId());
            return true;
        } catch (Exception e) {
            log.error("[M10] Directive push failed, directiveId={}, operatorId={}",
                    directive.getDirectiveId(), directive.getOperatorId(), e);
            return false;
        }
    }
}
