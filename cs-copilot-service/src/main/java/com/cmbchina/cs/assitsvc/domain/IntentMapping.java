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

    /** 配置版本号 */
    private String versionId;

    /** 主键 UUID */
    private String mappingId;

    /** AI 意图代码 */
    private String standardIntentCode;

    /** AI 意图名称（仅展示用） */
    private String standardIntentName;

    /** 关联 Copilot 动作 ID */
    private String actionId;

    /** 同意图多候选优先级，倒序取最高 */
    private Integer mappingPriority;

    /** 是否启用：Y / N */
    private String enabled;
}
