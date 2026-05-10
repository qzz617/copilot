package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 功能引用，存储于 CLOB copilotIndex.intentToItems 中，含 itemId + priority。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemReference {

    /** 功能 ID */
    private Long itemId;

    /** 意图-功能映射优先级 */
    private Integer priority;
}
