package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已执行步骤，坐席 ACCEPTED 反馈后追加，作为 AI 后续决策辅助信号。
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

    /** 追加时间，epoch ms */
    private Long timestamp;
}
