package com.cmbchina.cs.assitsvc.core.event;

import com.alibaba.fastjson2.JSON;
import com.cmbchina.cs.assitsvc.domain.FeedbackRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 反馈日志监听器。
 *
 * <p>无论 ES 是否启用，反馈都会在应用日志中留痕，作为故障排查的可靠兜底。
 */
@Slf4j
@Component
@Order(20)
public class FeedbackLogListener {

    /**
     * 记录坐席反馈事件。
     */
    @EventListener
    public void onFeedbackReceived(FeedbackReceivedEvent event) {
        try {
            FeedbackRequest request = event.getRequest();
            log.info("[M16] Copilot feedback log: {}", JSON.toJSONString(request));
        } catch (Exception e) {
            log.warn("[M16] Log feedback failed", e);
        }
    }
}
