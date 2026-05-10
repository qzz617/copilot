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

    /** 功能 ID，前端权限校验用 */
    private Long itemId;

    /** 功能名称 */
    private String itemName;

    /** 功能路径，用于卡片展示 */
    private String functionPath;
}
