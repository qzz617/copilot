package com.cmbchina.cs.assitsvc.core.event;

import com.cmbchina.cs.assitsvc.domain.FeedbackRequest;
import com.cmbchina.cs.assitsvc.infra.metrics.FeedbackEsClient;
import com.cmbchina.cs.assitsvc.infra.metrics.FeedbackLogRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * 反馈写 ES 监听器。
 *
 * <p>使用专用的 {@code feedbackMetricsExecutor} 线程池异步写入，避免阻塞反馈 HTTP 接口。
 * 队列满或 ES 故障时仅打 warn 日志，不影响反馈接口的成功响应（日志由 {@link FeedbackLogListener} 兜底）。
 *
 * <p><b>已知 TODO（MVP 阶段不做）</b>：
 * <ul>
 *   <li>{@code triggerLogId} 字段未反查 trigger log，始终为 null；</li>
 *   <li>{@code isEffective} 字段未做幂等判定，始终标记为 'N'。</li>
 * </ul>
 * 待后期接入持久化的 trigger log 后再实现。
 */
@Slf4j
@Component
@Order(30)
public class FeedbackEsListener {

    private final FeedbackEsClient feedbackEsClient;
    private final Executor feedbackMetricsExecutor;

    public FeedbackEsListener(FeedbackEsClient feedbackEsClient,
                              @Qualifier("feedbackMetricsExecutor") Executor feedbackMetricsExecutor) {
        this.feedbackEsClient = feedbackEsClient;
        this.feedbackMetricsExecutor = feedbackMetricsExecutor;
    }

    /**
     * 监听反馈事件，提交异步 ES 写入任务。
     */
    @EventListener
    public void onFeedbackReceived(FeedbackReceivedEvent event) {
        FeedbackRequest request = event.getRequest();
        FeedbackLogRecord record;
        try {
            record = FeedbackLogRecord.builder()
                    .logId(generateId())
                    .directiveId(request.getDirectiveId())
                    .triggerLogId(null)
                    .callId(request.getCallId())
                    .operatorId(request.getOperatorId())
                    .feedbackType(request.getFeedbackType())
                    .intentCode(request.getIntentCode())
                    .actionId(request.getActionId())
                    .menuItemId(request.getMenuItemId())
                    .isEffective("N")
                    .feedbackTime(parseInstant(request.getFeedbackTime()))
                    .build();
        } catch (Exception e) {
            log.warn("[M16] Build feedback record failed, directiveId={}", request.getDirectiveId(), e);
            return;
        }

        try {
            feedbackMetricsExecutor.execute(() -> indexFeedback(record));
        } catch (TaskRejectedException e) {
            log.warn("[M16] Feedback metrics queue full, directiveId={}", request.getDirectiveId(), e);
        } catch (Exception e) {
            log.warn("[M16] Submit feedback metrics failed, directiveId={}", request.getDirectiveId(), e);
        }
    }

    private void indexFeedback(FeedbackLogRecord record) {
        try {
            feedbackEsClient.index(record);
        } catch (Exception e) {
            log.warn("[M16] Index feedback to ES failed, directiveId={}", record.getDirectiveId(), e);
        }
    }

    private static String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private Instant parseInstant(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Instant.now();
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            log.warn("[M16] Parse instant failed, fallback to now");
            return Instant.now();
        }
    }
}
