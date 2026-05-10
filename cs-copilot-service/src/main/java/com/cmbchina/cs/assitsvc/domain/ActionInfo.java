package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 指令-动作信息，DirectiveDTO 的 action 子对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionInfo {

    /** 打开目标类型：URL / ROUTE / COMPONENT / IFRAME / NEW_WINDOW */
    private String targetKind;

    /** 打开方式：CURRENT_TAB / NEW_TAB / POPUP / DRAWER / WINDOW / IFRAME */
    private String openMode;

    /** 派生动作类型，前端按此选择执行器，如 OPEN_IFRAME */
    private String actionType;

    /** 含 ${COOKIE.xxx} 占位符的完整 URL */
    private String url;

    /** 参数 map，值可能含 ${COOKIE.xxx} 占位符 */
    private Map<String, String> params;
}
