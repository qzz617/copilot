package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * CLOB 反向索引，存于 MenuVersionData.copilotIndex，加速意图-功能查找。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopilotIndex {

    /** 意图代码 → 候选功能引用列表（含优先级），key 为 intentCode */
    private Map<String, List<ItemReference>> intentToItems;

    /** 功能 ID → 完整配置，key 为 itemId */
    private Map<Long, ItemFullConfig> itemById;
}
