package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指令-功能信息，DirectiveDTO 的 function 子对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionInfo {

    /** Copilot 动作 ID */
    private String actionId;

    /** Copilot 动作名称 */
    private String actionName;

    /** 可选快捷导航菜单 ID；纯意图唤起动作为空 */
    private Long menuItemId;

    /** 功能路径，用于卡片展示 */
    private String functionPath;
}
