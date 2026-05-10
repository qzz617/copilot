package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推送指令，通过 WebSocket 推送给前端坐席工作台。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectiveDTO {

    /** 指令唯一 ID，前端去重用 */
    private String directiveId;

    /** 指令类型：RECOMMENDATION / WEAK_HINT */
    private String directiveType;

    /** 通话 ID，前端校验是否当前通话 */
    private String callId;

    /** 坐席工号，前端校验是否当前坐席 */
    private String operatorId;

    /** 配置版本，便于 Bad Case 复现 */
    private String configVersion;

    /** 指令过期时间，ISO 8601，前端校验过期不执行 */
    private String expireAt;

    /** 意图信息 */
    private IntentInfo intent;

    /** 功能信息 */
    private FunctionInfo function;

    /** 展示信息 */
    private DisplayInfo display;

    /** 动作信息 */
    private ActionInfo action;

    /** 风险信息 */
    private RiskInfo risk;
}
