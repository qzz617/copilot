package com.cmbchina.cs.assitsvc.core.feedback;

import com.cmbchina.cs.assitsvc.core.intent.ExecutedStepsManager;
import com.cmbchina.cs.assitsvc.domain.FeedbackRequest;
import com.cmbchina.cs.assitsvc.infra.metrics.FeedbackLogDao;
import com.cmbchina.cs.assitsvc.infra.metrics.MetricsService;
import com.cmbchina.cs.assitsvc.infra.metrics.TriggerLogDao;
import com.cmbchina.cs.assitsvc.infra.metrics.TriggerLogRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Objects;

/**
 * 反馈处理服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private static final int IGNORED_MUTE_TTL_SECONDS = 120;

    private final TriggerLogDao triggerLogDao;
    private final FeedbackLogDao feedbackLogDao;
    private final ExecutedStepsManager stepsManager;
    private final MuteListManager muteListManager;
    private final MetricsService metricsService;

    @Override
    public FeedbackResult handleFeedback(FeedbackRequest request) {
        FeedbackResult validationResult = validateBasic(request);
        if (validationResult != null) {
            return validationResult;
        }

        TriggerLogRecord triggerLog = triggerLogDao.findByDirectiveId(request.getDirectiveId());
        FeedbackResult directiveResult = validateDirective(request, triggerLog);
        if (directiveResult != null) {
            return directiveResult;
        }

        boolean alreadyEffective = feedbackLogDao.existsEffective(request.getDirectiveId());
        boolean effective = !alreadyEffective
                && triggerLogDao.markDirectiveConsumedIfOpen(request.getDirectiveId());
        metricsService.recordFeedback(request, triggerLog, effective);

        if (!effective) {
            return FeedbackResult.success("DUPLICATE_RECORDED");
        }

        applyEffectiveFeedback(request);
        return FeedbackResult.success("EFFECTIVE");
    }

    private FeedbackResult validateBasic(FeedbackRequest request) {
        if (request == null) {
            return FeedbackResult.fail("INVALID_REQUEST", "request is null");
        }
        if (!StringUtils.hasText(request.getDirectiveId())
                || !StringUtils.hasText(request.getCallId())
                || !StringUtils.hasText(request.getOperatorId())
                || !StringUtils.hasText(request.getFeedbackType())
                || !StringUtils.hasText(request.getIntentCode())
                || !StringUtils.hasText(request.getActionId())) {
            return FeedbackResult.fail("INVALID_REQUEST", "required field missing");
        }
        if (!isSupportedFeedbackType(request.getFeedbackType())) {
            return FeedbackResult.fail("INVALID_FEEDBACK_TYPE", request.getFeedbackType());
        }
        return null;
    }

    private FeedbackResult validateDirective(FeedbackRequest request, TriggerLogRecord triggerLog) {
        if (triggerLog == null) {
            return FeedbackResult.fail("DIRECTIVE_NOT_FOUND");
        }
        if (triggerLog.getExpireAt() != null && Instant.now().isAfter(triggerLog.getExpireAt())) {
            return FeedbackResult.fail("DIRECTIVE_EXPIRED");
        }
        if (!Objects.equals(triggerLog.getCallId(), request.getCallId())
                || !Objects.equals(triggerLog.getOperatorId(), request.getOperatorId())
                || !Objects.equals(triggerLog.getIntentCode(), request.getIntentCode())
                || !Objects.equals(triggerLog.getActionId(), request.getActionId())) {
            log.warn("[M11] Feedback context mismatch, directiveId={}", request.getDirectiveId());
            return FeedbackResult.fail("CONTEXT_MISMATCH");
        }
        return null;
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

    private static boolean isSupportedFeedbackType(String feedbackType) {
        return "ACCEPTED".equals(feedbackType)
                || "IGNORED".equals(feedbackType)
                || "WRONG_INTENT".equals(feedbackType)
                || "WRONG_FUNCTION".equals(feedbackType);
    }
}
