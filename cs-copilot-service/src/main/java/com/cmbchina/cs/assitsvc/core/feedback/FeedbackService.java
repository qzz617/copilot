package com.cmbchina.cs.assitsvc.core.feedback;

import com.cmbchina.cs.assitsvc.domain.FeedbackRequest;

/**
 * 反馈处理服务。
 */
public interface FeedbackService {

    /**
     * 处理前端反馈。
     *
     * @param request 反馈请求
     * @return 处理结果
     */
    FeedbackResult handleFeedback(FeedbackRequest request);
}
