package com.cmbchina.cs.assitsvc.core.event;

import com.cmbchina.cs.assitsvc.core.feedback.MuteListManager;
import com.cmbchina.cs.assitsvc.core.intent.ExecutedStepsManager;
import com.cmbchina.cs.assitsvc.domain.FeedbackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 反馈业务闭环监听器。
 *
 * <p>根据坐席反馈类型对本通话上下文做出业务调整：
 * <ul>
 *   <li><b>ACCEPTED</b>：将本次意图追加到 executedSteps，供后续 AI 决策参考；</li>
 *   <li><b>WRONG_INTENT</b>：将该意图加入本通话静默列表，避免再次误判触发；</li>
 *   <li><b>IGNORED / WRONG_FUNCTION</b>：MVP 阶段不做处理（前者语义偏弱，后者后期再决定按
 *       actionId 静默还是按动作类静默），仅打 debug 日志。</li>
 * </ul>
 *
 * <p>本类不感知推送、日志、ES 等其他订阅者，与它们并列消费同一事件。
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class FeedbackBusinessListener {

    private final ExecutedStepsManager executedStepsManager;
    private final MuteListManager muteListManager;

    /**
     * 监听反馈事件，按反馈类型派发业务动作。
     *
     * <p>FeedbackServiceImpl 已校验过 callId / intentCode / actionId 等关键字段非空，
     * 本类不再二次校验，仅捕获下游异常避免影响其他监听器。
     */
    @EventListener
    public void onFeedbackReceived(FeedbackReceivedEvent event) {
        FeedbackRequest request = event.getRequest();
        String feedbackType = request.getFeedbackType();

        try {
            switch (feedbackType) {
                case "ACCEPTED":
                    // actionName 暂未在 FeedbackRequest 中收集，传 null；actionId 已足够标识动作
                    executedStepsManager.appendStep(
                            request.getCallId(),
                            request.getIntentCode(),
                            request.getIntentName(),
                            request.getActionId(),
                            null);
                    log.info("[M11] Executed step appended on ACCEPTED feedback, callId={}, intentCode={}, actionId={}",
                            request.getCallId(), request.getIntentCode(), request.getActionId());
                    break;
                case "WRONG_INTENT":
                    muteListManager.muteIntentForCall(
                            request.getCallId(),
                            request.getIntentCode());
                    log.info("[M11] Intent muted on WRONG_INTENT feedback, callId={}, intentCode={}",
                            request.getCallId(), request.getIntentCode());
                    break;
                case "IGNORED":
                case "WRONG_FUNCTION":
                    log.debug("[M11] Feedback type accepted but no business action in MVP, callId={}, type={}",
                            request.getCallId(), feedbackType);
                    break;
                default:
                    // 不应该到达，FeedbackServiceImpl 已限制白名单；保留兜底防止后期新增类型遗漏
                    log.warn("[M11] Unknown feedback type, callId={}, type={}",
                            request.getCallId(), feedbackType);
            }
        } catch (Exception e) {
            log.warn("[M11] Feedback business action failed, callId={}, type={}",
                    request.getCallId(), feedbackType, e);
        }
    }
}
