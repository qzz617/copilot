package com.cmbchina.cs.assitsvc.config.model;

import lombok.Data;

/**
 * cs_menu_item 数据库行，供关联菜单项的 action 做目标解析和校验。
 */
@Data
public class CopilotMenuItemRow {

    private Long itemId;
    private String itemName;
    private String url;
    private String sysFlag;
    private String pageId;
    private String pageTitle;
    private String enabled;
}
