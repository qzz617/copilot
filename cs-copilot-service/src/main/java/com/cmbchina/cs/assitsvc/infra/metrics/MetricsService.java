package com.cmbchina.cs.assitsvc.infra.metrics;

import com.alibaba.fastjson2.JSON;
import com.cmbchina.cs.assitsvc.domain.CallSession;
import com.cmbchina.cs.assitsvc.domain.DirectiveDTO;
import com.cmbchina.cs.assitsvc.domain.FeedbackRequest;
import com.cmbchina.cs.assitsvc.domain.ItemCandidate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * 业务监控埋点服务。
 */
@Slf4j
@Service
public class MetricsService {

    private final FeedbackEsClient feedbackEsClient;
    private final Executor feedbackMetricsExecutor;

    public MetricsService(FeedbackEsClient feedbackEsClient,
                          @Qualifier("feedbackMetricsExecutor") Executor feedbackMetricsExecutor) {
        this.feedbackEsClient = feedbackEsClient;
        this.feedbackMetricsExecutor = feedbackMetricsExecutor;
    }

    /**
     * 记录成功推送日志。
     */
    public void recordTriggerSuccess(DirectiveDTO directive, CallSession session,
                                     ItemCandidate candidate, int candidateCount) {
        if (directive == null) {
            return;
        }
        try {
            TriggerLogRecord record = TriggerLogRecord.builder()
                    .logId(generateId())
                    .callId(directive.getCallId())
                    .operatorId(directive.getOperatorId())
                    .customerId(session == null ? null : session.getCustomerId())
                    .intentCode(directive.getIntent() == null ? null : directive.getIntent().getIntentCode())
                    .intentName(directive.getIntent() == null ? null : directive.getIntent().getIntentName())
                    .actionId(directive.getFunction() == null ? null : directive.getFunction().getActionId())
                    .actionName(directive.getFunction() == null ? null : directive.getFunction().getActionName())
                    .menuItemId(directive.getFunction() == null ? null : directive.getFunction().getMenuItemId())
                    .candidateCount(candidateCount)
                    .riskLevel(directive.getRisk() == null ? null : directive.getRisk().getRiskLevel())
                    .directiveId(directive.getDirectiveId())
                    .expireAt(parseInstant(directive.getExpireAt()))
                    .directiveStatus("PUBLISHED")
                    .resultStatus("SUCCESS")
                    .triggerTime(Instant.now())
                    .configVersion(directive.getConfigVersion())
                    .build();
            log.info("[M16] Copilot trigger log: {}", JSON.toJSONString(record));
        } catch (Exception e) {
            log.warn("[M16] Record trigger success failed, directiveId={}", directive.getDirectiveId(), e);
        }
    }

    /**
     * 记录失败或过滤日志。
     */
    public void recordTriggerFailure(String callId, CallSession session, String intentCode, String intentName,
                                     String reasonCode, String filterStage, String configVersion) {
        try {
            TriggerLogRecord record = TriggerLogRecord.builder()
                    .logId(generateId())
                    .callId(callId)
                    .operatorId(session == null ? null : session.getOperatorId())
                    .customerId(session == null ? null : session.getCustomerId())
                    .intentCode(intentCode)
                    .intentName(intentName)
                    .resultStatus("FAIL")
                    .reasonCode(reasonCode)
                    .filterStage(filterStage)
                    .triggerTime(Instant.now())
                    .configVersion(configVersion)
                    .build();
            log.info("[M16] Copilot trigger log: {}", JSON.toJSONString(record));
        } catch (Exception e) {
            log.warn("[M16] Record trigger failure failed, callId={}, reasonCode={}", callId, reasonCode, e);
        }
    }

    /**
     * 记录反馈日志。
     */
    public String recordFeedback(FeedbackRequest request, TriggerLogRecord triggerLog, boolean effective) {
        String logId = generateId();
        try {
            FeedbackLogRecord record = FeedbackLogRecord.builder()
                    .logId(logId)
                    .directiveId(request.getDirectiveId())
                    .triggerLogId(triggerLog == null ? null : triggerLog.getLogId())
                    .callId(request.getCallId())
                    .operatorId(request.getOperatorId())
                    .feedbackType(request.getFeedbackType())
                    .intentCode(request.getIntentCode())
                    .actionId(request.getActionId())
                    .menuItemId(request.getMenuItemId())
                    .isEffective(effective ? "Y" : "N")
                    .feedbackTime(parseInstant(request.getFeedbackTime()))
                    .build();
            feedbackMetricsExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    indexFeedback(record);
                }
            });
            return logId;
        } catch (TaskRejectedException e) {
            log.warn("[M16] Feedback metrics queue full, directiveId={}", request.getDirectiveId(), e);
            return null;
        } catch (Exception e) {
            log.warn("[M16] Submit feedback metrics failed, directiveId={}", request.getDirectiveId(), e);
            return null;
        }
    }

    private void indexFeedback(FeedbackLogRecord record) {
        try {
            feedbackEsClient.index(record);
        } catch (Exception e) {
            log.warn("[M16] Record feedback failed, directiveId={}", record.getDirectiveId(), e);
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
