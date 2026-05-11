package com.cmbchina.cs.assitsvc.api.controller;

import com.cmbchina.cs.assitsvc.api.dto.ApiResult;
import com.cmbchina.cs.assitsvc.core.feedback.FeedbackResult;
import com.cmbchina.cs.assitsvc.core.feedback.FeedbackService;
import com.cmbchina.cs.assitsvc.domain.FeedbackRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Copilot 反馈接口。
 */
@RestController
@RequestMapping("/copilot")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * 采集前端反馈。
     *
     * @param request 反馈请求
     * @return 处理结果
     */
    @PostMapping("/feedback")
    public ApiResult feedback(@RequestBody FeedbackRequest request) {
        FeedbackResult result = feedbackService.handleFeedback(request);
        if (result.isSuccess()) {
            return ApiResult.builder().code("0000").message(result.getMessage()).build();
        }
        return ApiResult.fail("4000", result.getMessage(), result.getDetails());
    }
}
