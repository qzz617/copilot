package com.cmbchina.cs.assitsvc.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cmbchina.cs.assitsvc.config.mapper.CopilotConfigMapper;
import com.cmbchina.cs.assitsvc.config.model.CopilotActionRow;
import com.cmbchina.cs.assitsvc.config.model.CopilotMenuItemRow;
import com.cmbchina.cs.assitsvc.domain.ActionReference;
import com.cmbchina.cs.assitsvc.domain.CopilotActionConfig;
import com.cmbchina.cs.assitsvc.domain.CopilotConfigSnapshot;
import com.cmbchina.cs.assitsvc.domain.IntentMapping;
import com.cmbchina.cs.assitsvc.domain.ItemParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于 MyBatis 的 Copilot 独立配置读取仓储。
 */
@Repository
@RequiredArgsConstructor
public class MybatisCopilotConfigRepository implements CopilotConfigRepository {

    private final CopilotConfigMapper configMapper;

    @Override
    public String fetchLatestVersionMarker() {
        return configMapper.selectLatestPublishedVersion();
    }

    @Override
    public CopilotConfigSnapshot loadLatestSnapshot() {
        String versionId = fetchLatestVersionMarker();
        if (!StringUtils.hasText(versionId)) {
            throw new IllegalStateException("Latest copilot config version is empty");
        }

        List<CopilotActionRow> actionRows = defaultList(configMapper.selectEnabledActions());
        List<IntentMapping> mappings = defaultList(configMapper.selectEnabledMappings());

        Set<Long> menuItemIds = actionRows.stream()
                .map(CopilotActionRow::getMenuItemId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, CopilotMenuItemRow> menuItems = loadMenuItems(menuItemIds);

        Map<String, CopilotActionConfig> actionById = new LinkedHashMap<>();
        for (CopilotActionRow row : actionRows) {
            CopilotActionConfig action = toActionConfig(row, menuItems);
            actionById.put(action.getActionId(), action);
        }

        Map<String, List<ActionReference>> intentToActions = buildIntentIndex(mappings);
        return CopilotConfigSnapshot.builder()
                .versionId(versionId)
                .buildTime(Instant.now().toString())
                .intentToActions(intentToActions)
                .actionById(actionById)
                .build();
    }

    private Map<Long, CopilotMenuItemRow> loadMenuItems(Set<Long> menuItemIds) {
        if (menuItemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CopilotMenuItemRow> rows = defaultList(
                configMapper.selectMenuItems(new ArrayList<>(menuItemIds)));
        Map<Long, CopilotMenuItemRow> result = new LinkedHashMap<>();
        for (CopilotMenuItemRow row : rows) {
            result.put(row.getItemId(), row);
        }
        return result;
    }

    private CopilotActionConfig toActionConfig(CopilotActionRow row,
                                               Map<Long, CopilotMenuItemRow> menuItems) {
        CopilotMenuItemRow menuItem = null;
        if (row.getMenuItemId() != null) {
            menuItem = menuItems.get(row.getMenuItemId());
            validateMenuBinding(row, menuItem);
        }

        List<ItemParam> params = menuItem == null ? parseParamConfig(row.getParamConfigJson()) : null;

        return CopilotActionConfig.builder()
                .actionId(row.getActionId())
                .menuItemId(row.getMenuItemId())
                .itemSnapshotJson(row.getItemSnapshotJson())
                .actionName(row.getActionName())
                .enabled(isEnabled(row.getEnabled()))
                .functionPath(row.getFunctionPath())
                .targetKind(row.getTargetKind())
                .openMode(row.getOpenMode())
                .targetUrl(row.getTargetUrl())
                .routePath(row.getRoutePath())
                .windowFeature(row.getWindowFeature())
                .aiDisplayText(row.getAiDisplayText())
                .floatingTipText(row.getFloatingTipText())
                .riskLevel(row.getRiskLevel())
                .iconUrl(row.getIconUrl())
                .params(params)
                .build();
    }

    private void validateMenuBinding(CopilotActionRow row, CopilotMenuItemRow menuItem) {
        if (menuItem == null) {
            throw new IllegalStateException("menu item missing, actionId=" + row.getActionId()
                    + ", menuItemId=" + row.getMenuItemId());
        }
        if (!isEnabled(menuItem.getEnabled())) {
            throw new IllegalStateException("menu item disabled, actionId=" + row.getActionId()
                    + ", menuItemId=" + row.getMenuItemId());
        }
        if (StringUtils.hasText(row.getItemSnapshotJson())) {
            validateSnapshot(row, menuItem);
        }
    }

    private void validateSnapshot(CopilotActionRow row, CopilotMenuItemRow menuItem) {
        JSONObject snapshot = JSON.parseObject(row.getItemSnapshotJson());
        compareSnapshot(row, snapshot, "itemId", String.valueOf(menuItem.getItemId()));
        compareSnapshot(row, snapshot, "itemName", menuItem.getItemName());
        compareSnapshot(row, snapshot, "url", menuItem.getUrl());
        compareSnapshot(row, snapshot, "sysFlag", menuItem.getSysFlag());
        compareSnapshot(row, snapshot, "pageId", menuItem.getPageId());
        compareSnapshot(row, snapshot, "pageTitle", menuItem.getPageTitle());
        compareSnapshot(row, snapshot, "enabled", menuItem.getEnabled());
    }

    private void compareSnapshot(CopilotActionRow row, JSONObject snapshot, String field, String actual) {
        Object expected = snapshot.get(field);
        if (expected != null && !String.valueOf(expected).equals(String.valueOf(actual))) {
            throw new IllegalStateException("menu item snapshot mismatch, actionId=" + row.getActionId()
                    + ", menuItemId=" + row.getMenuItemId() + ", field=" + field);
        }
    }

    private List<ItemParam> parseParamConfig(String paramConfigJson) {
        if (!StringUtils.hasText(paramConfigJson)) {
            return null;
        }
        return JSON.parseArray(paramConfigJson, ItemParam.class);
    }

    private Map<String, List<ActionReference>> buildIntentIndex(List<IntentMapping> mappings) {
        Map<String, List<ActionReference>> result = new LinkedHashMap<>();
        for (IntentMapping mapping : mappings) {
            if (mapping == null || !StringUtils.hasText(mapping.getStandardIntentCode())) {
                continue;
            }
            List<ActionReference> refs = result.get(mapping.getStandardIntentCode());
            if (refs == null) {
                refs = new ArrayList<>();
                result.put(mapping.getStandardIntentCode(), refs);
            }
            refs.add(ActionReference.builder()
                    .actionId(mapping.getActionId())
                    .priority(mapping.getMappingPriority())
                    .build());
        }
        for (List<ActionReference> refs : result.values()) {
            Collections.sort(refs, new Comparator<ActionReference>() {
                @Override
                public int compare(ActionReference left, ActionReference right) {
                    return Integer.compare(priority(right), priority(left));
                }
            });
        }
        return result;
    }

    private static int priority(ActionReference ref) {
        return ref == null || ref.getPriority() == null ? 0 : ref.getPriority();
    }

    private static boolean isEnabled(String enabled) {
        if (!StringUtils.hasText(enabled)) {
            return false;
        }
        return "Y".equalsIgnoreCase(enabled.trim());
    }

    private static <T> List<T> defaultList(List<T> list) {
        return list == null ? Collections.<T>emptyList() : list;
    }
}
