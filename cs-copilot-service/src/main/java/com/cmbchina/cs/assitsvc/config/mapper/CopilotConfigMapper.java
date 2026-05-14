package com.cmbchina.cs.assitsvc.config.mapper;

import com.cmbchina.cs.assitsvc.config.model.CopilotActionRow;
import com.cmbchina.cs.assitsvc.config.model.CopilotMenuItemRow;
import com.cmbchina.cs.assitsvc.domain.IntentMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Copilot 独立配置表查询 Mapper。
 */
@Mapper
public interface CopilotConfigMapper {

    @Select("SELECT version_id FROM svccfg.cs_copilot_config_version "
            + "WHERE publish_status = 'PUBLISHED' ORDER BY created_time DESC LIMIT 1")
    String selectLatestPublishedVersion();

    @Select("SELECT action_id AS actionId, menu_item_id AS menuItemId, "
            + "item_snapshot_json AS itemSnapshotJson, action_name AS actionName, enabled, "
            + "function_path AS functionPath, target_kind AS targetKind, open_mode AS openMode, "
            + "target_url AS targetUrl, route_path AS routePath, window_feature AS windowFeature, "
            + "ai_display_text AS aiDisplayText, floating_tip_text AS floatingTipText, "
            + "risk_level AS riskLevel, icon_url AS iconUrl, param_config_json AS paramConfigJson "
            + "FROM svccfg.cs_copilot_action WHERE enabled = 'Y'")
    List<CopilotActionRow> selectEnabledActions();

    @Select("SELECT mapping_id AS mappingId, standard_intent_code AS standardIntentCode, "
            + "standard_intent_name AS standardIntentName, action_id AS actionId, "
            + "mapping_priority AS mappingPriority, enabled "
            + "FROM svccfg.cs_copilot_intent_mapping WHERE enabled = 'Y'")
    List<IntentMapping> selectEnabledMappings();

    @Select({
            "<script>",
            "SELECT item_id AS itemId, item_name AS itemName, url, sys_flag AS sysFlag, ",
            "page_id AS pageId, page_title AS pageTitle, enabled ",
            "FROM svccfg.cs_menu_item WHERE item_id IN ",
            "<foreach collection='itemIds' item='itemId' open='(' separator=',' close=')'>",
            "#{itemId}",
            "</foreach>",
            "</script>"
    })
    List<CopilotMenuItemRow> selectMenuItems(@Param("itemIds") List<Long> itemIds);

}
