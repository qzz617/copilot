package com.cmbchina.cs.assitsvc.config.model;

import lombok.Data;

/**
 * cs_menu_item_param 数据库行。
 */
@Data
public class CopilotMenuItemParamRow {

    private Long itemId;
    private String paramType;
    private String paramKey;
    private String paramValue;
}
