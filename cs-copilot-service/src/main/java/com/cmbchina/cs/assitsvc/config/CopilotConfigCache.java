package com.cmbchina.cs.assitsvc.config;

import com.alibaba.fastjson2.JSON;
import com.cmbchina.cs.assitsvc.domain.ActionReference;
import com.cmbchina.cs.assitsvc.domain.CopilotActionConfig;
import com.cmbchina.cs.assitsvc.domain.MenuVersionData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Copilot CLOB 配置缓存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CopilotConfigCache {

    private final MenuVersionDao menuVersionDao;
    private final CopilotConfigValidationService validationService;

    private volatile MenuVersionData currentVersion;
    private volatile Instant lastLoadTime;

    /**
     * 启动时尝试加载最新 CLOB。加载失败不阻断服务启动，运行时返回空候选。
     */
    @PostConstruct
    public void init() {
        try {
            loadLatestVersion();
        } catch (Exception e) {
            log.warn("[M07] Load initial copilot config failed, recommendations disabled until refresh", e);
        }
    }

    /**
     * 重新加载最新 CLOB 配置。
     */
    public synchronized void loadLatestVersion() {
        String configClob = menuVersionDao.fetchLatestActiveVersion();
        if (!StringUtils.hasText(configClob)) {
            throw new IllegalStateException("Latest menu version CLOB is empty");
        }

        MenuVersionData newVersion = JSON.parseObject(configClob, MenuVersionData.class);
        ConfigValidationResult validationResult = validationService.validate(newVersion);
        if (!validationResult.isSuccess()) {
            throw new IllegalStateException("Copilot config validation failed: " + validationResult.getErrors());
        }
        currentVersion = newVersion;
        lastLoadTime = Instant.now();
        log.info("[M07] Loaded copilot config, version={}, loadTime={}", newVersion.getVersion(), lastLoadTime);
    }

    /**
     * 按意图代码查找候选动作引用。
     *
     * @param intentCode 意图代码
     * @return 候选动作引用列表
     */
    public List<ActionReference> findCandidatesByIntent(String intentCode) {
        MenuVersionData versionData = currentVersion;
        if (versionData == null || versionData.getCopilotIndex() == null) {
            return Collections.emptyList();
        }

        Map<String, List<ActionReference>> intentToActions = versionData.getCopilotIndex().getIntentToActions();
        if (intentToActions == null) {
            return Collections.emptyList();
        }
        List<ActionReference> refs = intentToActions.get(intentCode);
        return refs == null ? Collections.<ActionReference>emptyList() : refs;
    }

    /**
     * 查询动作完整配置。
     *
     * @param actionId 动作 ID
     * @return 动作完整配置；不存在时返回 null
     */
    public CopilotActionConfig getActionConfig(String actionId) {
        MenuVersionData versionData = currentVersion;
        if (versionData == null || versionData.getCopilotIndex() == null) {
            return null;
        }

        Map<String, CopilotActionConfig> actionById = versionData.getCopilotIndex().getActionById();
        return actionById == null ? null : actionById.get(actionId);
    }

    /**
     * 获取当前 CLOB 版本号。
     *
     * @return 版本号；未加载时返回 null
     */
    public String getCurrentVersion() {
        MenuVersionData versionData = currentVersion;
        return versionData == null ? null : versionData.getVersion();
    }

    /**
     * 获取最近一次加载时间。
     *
     * @return 最近一次加载时间
     */
    public Instant getLastLoadTime() {
        return lastLoadTime;
    }

    /**
     * 统计当前意图映射数量。
     *
     * @return 意图映射数量
     */
    public int getIntentMappingCount() {
        MenuVersionData versionData = currentVersion;
        if (versionData == null) {
            return 0;
        }
        if (versionData.getCopilotIndex() == null || versionData.getCopilotIndex().getIntentToActions() == null) {
            return 0;
        }
        return versionData.getCopilotIndex().getIntentToActions().size();
    }

    /**
     * 统计当前启用 Copilot 的动作数量。
     *
     * @return 动作数量
     */
    public int getCopilotEnabledActionCount() {
        MenuVersionData versionData = currentVersion;
        if (versionData == null || versionData.getCopilotIndex() == null) {
            return 0;
        }
        Map<String, CopilotActionConfig> actionById = versionData.getCopilotIndex().getActionById();
        return actionById == null ? 0 : actionById.size();
    }
}
