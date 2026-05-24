package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 候选动作，意图-动作匹配后的结果，包含完整配置和优先级。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCandidate {

    /** Copilot 动作 ID */
    private String actionId;

    /** 映射优先级（倒序排列，值越大越优先） */
    private Integer priority;

    /** 动作完整配置 */
    private CopilotActionConfig config;
}
