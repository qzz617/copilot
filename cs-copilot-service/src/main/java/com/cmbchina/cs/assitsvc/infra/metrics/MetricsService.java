package com.cmbchina.cs.assitsvc.infra.metrics;

import com.cmbchina.cs.assitsvc.domain.CallSession;
import com.cmbchina.cs.assitsvc.domain.DirectiveDTO;
import com.cmbchina.cs.assitsvc.domain.FeedbackRequest;
import com.cmbchina.cs.assitsvc.domain.ItemCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * 业务监控埋点服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final TriggerLogDao triggerLogDao;
    private final FeedbackLogDao feedbackLogDao;

    /**
     * 记录成功推送日志。
     */
    public void recordTriggerSuccess(DirectiveDTO directive, CallSession session,
                                     ItemCandidate candidate, int candidateCount) {
        if (directive == null) {
            return;
        }
        try {
            triggerLogDao.insert(TriggerLogRecord.builder()
                    .logId(generateId())
                    .callId(directive.getCallId())
                    .operatorId(directive.getOperatorId())
                    .customerId(session == null ? null : session.getCustomerId())
                    .intentCode(directive.getIntent() == null ? null : directive.getIntent().getIntentCode())
                    .intentName(directive.getIntent() == null ? null : directive.getIntent().getIntentName())
                    .itemId(directive.getFunction() == null ? null : directive.getFunction().getItemId())
                    .itemName(directive.getFunction() == null ? null : directive.getFunction().getItemName())
                    .candidateCount(candidateCount)
                    .riskLevel(directive.getRisk() == null ? null : directive.getRisk().getRiskLevel())
                    .directiveId(directive.getDirectiveId())
                    .expireAt(parseInstant(directive.getExpireAt()))
                    .directiveStatus("PUSHED")
                    .aiSuccess("Y")
                    .resultStatus("SUCCESS")
                    .triggerTime(Instant.now())
                    .configVersion(directive.getConfigVersion())
                    .build());
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
            triggerLogDao.insert(TriggerLogRecord.builder()
                    .logId(generateId())
                    .callId(callId)
                    .operatorId(session == null ? null : session.getOperatorId())
                    .customerId(session == null ? null : session.getCustomerId())
                    .intentCode(intentCode)
                    .intentName(intentName)
                    .aiSuccess("N")
                    .resultStatus("FAIL")
                    .reasonCode(reasonCode)
                    .filterStage(filterStage)
                    .triggerTime(Instant.now())
                    .configVersion(configVersion)
                    .build());
        } catch (Exception e) {
            log.warn("[M16] Record trigger failure failed, callId={}, reasonCode={}", callId, reasonCode, e);
        }
    }

    /**
     * 记录反馈日志。
     */
    public void recordFeedback(FeedbackRequest request, TriggerLogRecord triggerLog, boolean effective) {
        try {
            feedbackLogDao.insert(FeedbackLogRecord.builder()
                    .logId(generateId())
                    .directiveId(request.getDirectiveId())
                    .triggerLogId(triggerLog == null ? null : triggerLog.getLogId())
                    .callId(request.getCallId())
                    .operatorId(request.getOperatorId())
                    .feedbackType(request.getFeedbackType())
                    .intentCode(request.getIntentCode())
                    .itemId(request.getItemId())
                    .isEffective(effective ? "Y" : "N")
                    .feedbackTime(parseInstant(request.getFeedbackTime()))
                    .build());
        } catch (Exception e) {
            log.warn("[M16] Record feedback failed, directiveId={}", request.getDirectiveId(), e);
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
