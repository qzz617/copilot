package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Copilot 可唤起动作配置，对应 CLOB copilotIndex.actionById 中的单条记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopilotActionConfig {

    /** Copilot 动作 ID */
    private String actionId;

    /** 可选快捷导航菜单 ID；纯意图唤起动作为空 */
    private Long menuItemId;

    /** 动作名称 */
    private String actionName;

    /** Copilot 是否启用 */
    private Boolean enabled;

    /** 功能路径，用于卡片展示 */
    private String functionPath;

    /** 打开目标类型：URL / ROUTE / IFRAME / NEW_WINDOW */
    private String targetKind;

    /** 打开方式：CURRENT_TAB / NEW_TAB / IFRAME / WINDOW */
    private String openMode;

    /** URL / IFRAME / NEW_WINDOW 场景的目标地址 */
    private String targetUrl;

    /** ROUTE 场景的前端路由路径 */
    private String routePath;

    /** 浮窗 AI 展示文字 */
    private String aiDisplayText;

    /** 跳转前提示文案 */
    private String floatingTipText;

    /** 风险等级：LOW / MEDIUM / HIGH / DISABLED */
    private String riskLevel;

    /** 图标 URL */
    private String iconUrl;

    /** 动作参数列表 */
    private List<ItemParam> params;
}
