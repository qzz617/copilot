package com.cmbchina.cs.assitsvc.infra.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * cs_copilot_feedback_log 记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackLogRecord {

    private String logId;
    private String directiveId;
    private String triggerLogId;
    private String callId;
    private String operatorId;
    private String feedbackType;
    private String intentCode;
    private Long itemId;
    private String frontendReason;
    private String isEffective;
    private Instant feedbackTime;
}
