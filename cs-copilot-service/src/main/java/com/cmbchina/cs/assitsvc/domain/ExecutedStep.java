package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已执行步骤，坐席 ACCEPTED 反馈后追加，作为 AI 后续决策辅助信号。
 *
 * <p>同一 intent 下可能挂多个候选 action，仅记录 intentCode 无法表达"到底执行了哪个动作"，
 * 因此同时保留 actionId/actionName 供 AI 区分。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutedStep {

    /** 意图代码 */
    private String intentCode;

    /** 意图名称 */
    private String intentName;

    /** 动作 ID */
    private String actionId;

    /** 动作名称 */
    private String actionName;

    /** 追加时间，epoch ms */
    private Long timestamp;
}
