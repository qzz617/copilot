package com.cmbchina.cs.assitsvc.core.feedback;

import com.cmbchina.cs.assitsvc.core.event.FeedbackReceivedEvent;
import com.cmbchina.cs.assitsvc.domain.FeedbackRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 反馈处理服务实现。
 *
 * <p>仅做基础字段校验，校验通过后发布 {@link FeedbackReceivedEvent}；
 * 实际持久化（日志、ES、未来的飞轮）由独立监听器承担。
 */
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public FeedbackResult handleFeedback(FeedbackRequest request) {
        FeedbackResult validationResult = validateBasic(request);
        if (validationResult != null) {
            return validationResult;
        }
        eventPublisher.publishEvent(new FeedbackReceivedEvent(request));
        return FeedbackResult.success("RECORDED");
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

    private static boolean isSupportedFeedbackType(String feedbackType) {
        return "ACCEPTED".equals(feedbackType)
                || "IGNORED".equals(feedbackType)
                || "WRONG_INTENT".equals(feedbackType)
                || "WRONG_FUNCTION".equals(feedbackType);
    }
}
