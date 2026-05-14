package com.cmbchina.cs.assitsvc.config.model;

import lombok.Data;

/**
 * cs_copilot_action 数据库行。
 */
@Data
public class CopilotActionRow {

    private String versionId;
    private String actionId;
    private Long menuItemId;
    private String itemSnapshotJson;
    private String actionName;
    private String enabled;
    private String functionPath;
    private String targetKind;
    private String openMode;
    private String targetUrl;
    private String routePath;
    private String windowFeature;
    private String aiDisplayText;
    private String floatingTipText;
    private String riskLevel;
    private String iconUrl;
    private String paramConfigJson;
}
