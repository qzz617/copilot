package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 意图识别结果，由 AI 接口响应解析而来。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentResult {

    /** AI 返回的意图代码 */
    private String intentCode;

    /** AI 返回的意图名称 */
    private String intentName;
}
