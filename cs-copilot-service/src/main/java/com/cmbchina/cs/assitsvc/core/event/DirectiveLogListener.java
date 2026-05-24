package com.cmbchina.cs.assitsvc.core.event;

import com.alibaba.fastjson2.JSON;
import com.cmbchina.cs.assitsvc.domain.CallSession;
import com.cmbchina.cs.assitsvc.domain.DirectiveDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 指令链路日志监听器。
 *
 * <p>MVP 阶段指令链路（成功/失败）的唯一记录形式：序列化为 JSON 行写入应用日志。
 * 后期接入 ES / Kafka 时新增独立监听器即可，无需改动主链路或本类。
 */
@Slf4j
@Component
@Order(20)
public class DirectiveLogListener {

    /**
     * 记录指令构建成功事件。
     */
    @EventListener
    public void onDirectivePrepared(DirectivePreparedEvent event) {
        try {
            DirectiveDTO directive = event.getDirective();
            CallSession session = event.getSession();
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("eventType", "DIRECTIVE_PREPARED");
            record.put("resultStatus", "SUCCESS");
            record.put("callId", directive.getCallId());
            record.put("operatorId", directive.getOperatorId());
            record.put("customerId", session == null ? null : session.getCustomerId());
            record.put("directiveId", directive.getDirectiveId());
            record.put("intentCode", directive.getIntent() == null ? null : directive.getIntent().getIntentCode());
            record.put("intentName", directive.getIntent() == null ? null : directive.getIntent().getIntentName());
            record.put("actionId", directive.getFunction() == null ? null : directive.getFunction().getActionId());
            record.put("actionName", directive.getFunction() == null ? null : directive.getFunction().getActionName());
            record.put("menuItemId", directive.getFunction() == null ? null : directive.getFunction().getMenuItemId());
            record.put("candidateCount", event.getCandidateCount());
            record.put("riskLevel", directive.getRisk() == null ? null : directive.getRisk().getRiskLevel());
            record.put("expireAt", directive.getExpireAt());
            record.put("configVersion", directive.getConfigVersion());
            log.info("[M16] Copilot directive log: {}", JSON.toJSONString(record));
        } catch (Exception e) {
            log.warn("[M16] Log directive prepared failed", e);
        }
    }

    /**
     * 记录指令构建失败事件。
     */
    @EventListener
    public void onDirectiveFailed(DirectiveFailedEvent event) {
        try {
            CallSession session = event.getSession();
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("eventType", "DIRECTIVE_FAILED");
            record.put("resultStatus", "FAIL");
            record.put("callId", event.getCallId());
            record.put("operatorId", session == null ? null : session.getOperatorId());
            record.put("customerId", session == null ? null : session.getCustomerId());
            record.put("intentCode", event.getIntentCode());
            record.put("intentName", event.getIntentName());
            record.put("reasonCode", event.getReasonCode());
            record.put("filterStage", event.getFilterStage());
            record.put("configVersion", event.getConfigVersion());
            log.info("[M16] Copilot directive log: {}", JSON.toJSONString(record));
        } catch (Exception e) {
            log.warn("[M16] Log directive failed event failed, callId={}", event.getCallId(), e);
        }
    }
}
