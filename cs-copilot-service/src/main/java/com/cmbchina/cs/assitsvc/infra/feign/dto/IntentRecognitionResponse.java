package com.cmbchina.cs.assitsvc.infra.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 意图识别响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentRecognitionResponse {

    /** 成功响应码。 */
    public static final String RESP_CODE_SUCCESS = "1000";

    /** 响应码，1000 表示成功。 */
    private String respCode;

    /** 响应消息。 */
    private String respMsg;

    /** 响应数据。 */
    private DataNode data;

    /**
     * AI 响应 data 节点。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataNode {

        /** 通话 ID。 */
        private String sessionId;

        /** 请求 ID。 */
        private String requestId;

        /** 意图名称。 */
        private String intentName;

        /** 意图代码。 */
        private String intentCode;

        /** 澄清内容，本期不使用。 */
        private String clarifyContent;

        /** LLM SOP 结果，本期不使用。 */
        private Object llmResults;
    }
}
