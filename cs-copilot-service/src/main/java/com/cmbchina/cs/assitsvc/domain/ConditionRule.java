package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 条件规则，支持 AND（all）和 OR（any）两种组合。
 * 异常时默认返回 false（DD-V1.2 P1-19）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionRule {

    /** AND 组合：所有条件都满足才匹配 */
    private List<ConditionItem> all;

    /** OR 组合：任一条件满足即匹配 */
    private List<ConditionItem> any;
}
