package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指令-风险信息，DirectiveDTO 的 risk 子对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskInfo {

    /** 风险等级：LOW / MEDIUM / HIGH */
    private String riskLevel;

    /** 是否需要坐席二次确认，MEDIUM/HIGH 时为 true */
    private Boolean needConfirm;
}
