package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指令构建上下文，聚合构建 DirectiveDTO 所需的所有输入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildContext {

    /** 通话 ID */
    private String callId;

    /** 坐席工号 */
    private String operatorId;

    /** 当前配置版本 */
    private String configVersion;

    /** 候选动作完整配置 */
    private CopilotActionConfig action;

    /** 参数解析上下文 */
    private ParamContext paramContext;
}
