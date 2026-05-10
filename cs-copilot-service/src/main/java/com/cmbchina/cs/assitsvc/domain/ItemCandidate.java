package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 候选项，意图-功能匹配后的结果，包含完整配置和优先级。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCandidate {

    /** 功能 ID */
    private Long itemId;

    /** 映射优先级（倒序排列，值越大越优先） */
    private Integer priority;

    /** 功能完整配置 */
    private ItemFullConfig config;
}
