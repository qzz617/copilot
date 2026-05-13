package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Copilot 运行时配置快照，由独立配置表加载后在本地内存构建。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopilotConfigSnapshot {

    /** Copilot 配置版本号 */
    private String versionId;

    /** 快照构建时间，ISO 8601 格式 */
    private String buildTime;

    /** 意图代码 → 候选动作引用列表（含优先级），key 为 intentCode */
    private Map<String, List<ActionReference>> intentToActions;

    /** 动作 ID → 完整配置 */
    private Map<String, CopilotActionConfig> actionById;
}
