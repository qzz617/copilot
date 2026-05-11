package com.cmbchina.cs.assitsvc.infra.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * cs_copilot_trigger_log 记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerLogRecord {

    private String logId;
    private String callId;
    private String operatorId;
    private String customerId;
    private String intentCode;
    private String intentName;
    private Long itemId;
    private String itemName;
    private Integer candidateCount;
    private String riskLevel;
    private String directiveId;
    private Instant expireAt;
    private String directiveStatus;
    private String aiRequestId;
    private Long aiResponseTimeMs;
    private String aiSuccess;
    private String aiFailureReason;
    private String resultStatus;
    private String reasonCode;
    private String filterStage;
    private String missingParamsJson;
    private Double asrConfidence;
    private String asrTextHash;
    private Instant triggerTime;
    private String configVersion;
}
