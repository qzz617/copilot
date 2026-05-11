package com.cmbchina.cs.assitsvc.core.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 反馈处理结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResult {

    private boolean success;
    private String message;
    private String details;

    public static FeedbackResult success(String message) {
        return FeedbackResult.builder().success(true).message(message).build();
    }

    public static FeedbackResult fail(String message) {
        return FeedbackResult.builder().success(false).message(message).build();
    }

    public static FeedbackResult fail(String message, String details) {
        return FeedbackResult.builder().success(false).message(message).details(details).build();
    }
}
