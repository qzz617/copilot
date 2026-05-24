package com.cmbchina.cs.assitsvc.core.event;

import com.cmbchina.cs.assitsvc.domain.FeedbackRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 坐席反馈到达事件。
 *
 * <p>{@link com.cmbchina.cs.assitsvc.core.feedback.FeedbackServiceImpl} 校验通过后发布本事件，
 * 由各监听器决定后续处理（日志、写 ES、未来的数据飞轮等）。
 */
@Getter
@RequiredArgsConstructor
public class FeedbackReceivedEvent {

    /** 校验通过的反馈请求。 */
    private final FeedbackRequest request;
}
