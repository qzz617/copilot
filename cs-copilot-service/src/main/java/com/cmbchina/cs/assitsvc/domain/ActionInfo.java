package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 指令-动作信息，DirectiveDTO 的 action 子对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionInfo {

    /** 目标来源：ACTION / MENU_ITEM */
    private String targetSource;

    /** 打开目标类型：URL / ROUTE / IFRAME / NEW_WINDOW */
    private String targetKind;

    /** 打开方式：CURRENT_TAB / NEW_TAB / WINDOW / IFRAME */
    private String openMode;

    /** 派生动作类型，前端按此选择执行器，如 OPEN_IFRAME */
    private String actionType;

    /** 目标基础 URL / routePath；纯 action 不在后端拼接客户参数。 */
    private String url;

    /** 后端已解析参数 map；当前纯 action 不在后端解析客户参数，通常为空。 */
    private Map<String, String> params;

    /** 参数配置，前端按 paramType 从 Cookie/工作台上下文取值。 */
    private List<ItemParam> paramConfigs;
}
