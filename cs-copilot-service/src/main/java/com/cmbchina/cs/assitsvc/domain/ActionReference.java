package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 动作引用，存储于 CLOB copilotIndex.intentToActions 中，含 actionId + priority。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionReference {

    /** Copilot 动作 ID */
    private String actionId;

    /** 意图-动作映射优先级 */
    private Integer priority;
}
