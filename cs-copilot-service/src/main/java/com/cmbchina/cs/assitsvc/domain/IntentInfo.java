package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指令-意图信息，DirectiveDTO 的 intent 子对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentInfo {

    /** AI 返回的意图代码 */
    private String intentCode;

    /** 意图名称 */
    private String intentName;
}
