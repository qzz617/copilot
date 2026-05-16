package com.cmbchina.cs.assitsvc.core.feedback;

import com.cmbchina.cs.assitsvc.core.intent.ExecutedStepsManager;
import com.cmbchina.cs.assitsvc.domain.FeedbackRequest;
import com.cmbchina.cs.assitsvc.infra.metrics.FeedbackLogDao;
import com.cmbchina.cs.assitsvc.infra.metrics.TriggerLogDao;
import com.cmbchina.cs.assitsvc.infra.metrics.TriggerLogRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * 异步处理反馈生效逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackEffectProcessor {

    private static final int IGNORED_MUTE_TTL_SECONDS = 120;

    private final TriggerLogDao triggerLogDao;
    private final FeedbackLogDao feedbackLogDao;
    private final ExecutedStepsManager stepsManager;
    private final MuteListManager muteListManager;

    /**
     * 异步校验指令并让首条合法反馈生效。
     *
     * @param request       反馈请求
     * @param feedbackLogId 已记录的反馈日志 ID，日志记录失败时为空
     */
    @Async("feedbackEffectExecutor")
    public void applyAsync(FeedbackRequest request, String feedbackLogId) {
        try {
            apply(request, feedbackLogId);
        } catch (Exception e) {
            log.warn("[M11] Async feedback effect failed, directiveId={}",
                    request == null ? null : request.getDirectiveId(), e);
        }
    }

    private void apply(FeedbackRequest request, String feedbackLogId) {
        TriggerLogRecord triggerLog = triggerLogDao.findByDirectiveId(request.getDirectiveId());
        if (!isDirectiveValid(request, triggerLog)) {
            return;
        }

        if (!triggerLogDao.markDirectiveConsumedIfOpen(request.getDirectiveId())) {
            log.info("[M11] Feedback not effective, directive already consumed or expired, directiveId={}",
                    request.getDirectiveId());
            return;
        }

        applyEffectiveFeedback(request);
        markFeedbackEffective(feedbackLogId, triggerLog);
    }

    private boolean isDirectiveValid(FeedbackRequest request, TriggerLogRecord triggerLog) {
        if (triggerLog == null) {
            log.info("[M11] Feedback ignored because directive not found, directiveId={}", request.getDirectiveId());
            return false;
        }
        if (triggerLog.getExpireAt() != null && Instant.now().isAfter(triggerLog.getExpireAt())) {
            log.info("[M11] Feedback ignored because directive expired, directiveId={}", request.getDirectiveId());
            return false;
        }
        if (!Objects.equals(triggerLog.getCallId(), request.getCallId())
                || !Objects.equals(triggerLog.getOperatorId(), request.getOperatorId())
                || !Objects.equals(triggerLog.getIntentCode(), request.getIntentCode())
                || !Objects.equals(triggerLog.getActionId(), request.getActionId())) {
            log.warn("[M11] Feedback context mismatch, directiveId={}", request.getDirectiveId());
            return false;
        }
        return true;
    }

    private void applyEffectiveFeedback(FeedbackRequest request) {
        String type = request.getFeedbackType();
        if ("ACCEPTED".equals(type)) {
            stepsManager.appendStep(request.getCallId(), request.getIntentCode(), request.getIntentName());
        } else if ("IGNORED".equals(type)) {
            muteListManager.muteIntent(request.getCallId(), request.getIntentCode(), IGNORED_MUTE_TTL_SECONDS);
        } else if ("WRONG_INTENT".equals(type)) {
            muteListManager.muteIntentForCall(request.getCallId(), request.getIntentCode());
        } else if ("WRONG_FUNCTION".equals(type)) {
            muteListManager.muteActionForCall(request.getCallId(), request.getActionId());
        }
    }

    private void markFeedbackEffective(String feedbackLogId, TriggerLogRecord triggerLog) {
        if (feedbackLogId == null) {
            return;
        }
        try {
            feedbackLogDao.markEffective(feedbackLogId, triggerLog.getLogId());
        } catch (Exception e) {
            log.warn("[M11] Mark feedback log effective failed, feedbackLogId={}, directiveId={}",
                    feedbackLogId, triggerLog.getDirectiveId(), e);
        }
    }
}
