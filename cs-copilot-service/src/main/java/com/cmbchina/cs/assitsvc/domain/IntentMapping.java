package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 意图映射，对应 cs_copilot_intent_mapping 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentMapping {

    /** 主键 UUID */
    private String mappingId;

    /** AI 意图代码 */
    private String standardIntentCode;

    /** AI 意图名称（仅展示用） */
    private String standardIntentName;

    /** 关联功能 ID */
    private Long itemId;

    /** 同意图多候选优先级，倒序取最高 */
    private Integer mappingPriority;

    /** 附加条件规则 JSON 字符串 */
    private String conditionRule;

    /** 是否启用：Y / N */
    private String enabled;
}
