package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 反馈请求，对应 POST /copilot/feedback 请求体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {

    /** 来自推送指令的 directiveId */
    private String directiveId;

    /** 通话 ID */
    private String callId;

    /** 坐席工号 */
    private String operatorId;

    /** 反馈类型：ACCEPTED / IGNORED / WRONG_INTENT / WRONG_FUNCTION */
    private String feedbackType;

    /** 推送时的意图代码 */
    private String intentCode;

    /** 意图名称 */
    private String intentName;

    /** 功能 ID */
    private Long itemId;

    /** 反馈时间，ISO 8601 格式 */
    private String feedbackTime;
}
