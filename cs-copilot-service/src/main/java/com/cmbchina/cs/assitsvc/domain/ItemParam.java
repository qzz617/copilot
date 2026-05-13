package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 动作参数，对应快捷导航参数或 cs_copilot_action.param_config_json 中一项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemParam {

    /** 参数类型，对应 StandardParamType 枚举名 */
    private String paramType;

    /** URL 参数名（key） */
    private String paramKey;

    /** 参数值：LITERAL 场景为字面值，COOKIE 场景为 cookie 字段名 */
    private String paramValue;
}
