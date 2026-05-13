package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * CLOB 反向索引，存于 MenuVersionData.copilotIndex，加速意图-动作查找。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopilotIndex {

    /** 意图代码 → 候选动作引用列表（含优先级），key 为 intentCode */
    private Map<String, List<ActionReference>> intentToActions;

    /** 动作 ID → 完整配置 */
    private Map<String, CopilotActionConfig> actionById;
}
