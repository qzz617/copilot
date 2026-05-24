package com.cmbchina.cs.assitsvc.config;

import com.cmbchina.cs.assitsvc.domain.ActionReference;
import com.cmbchina.cs.assitsvc.domain.CopilotActionConfig;
import com.cmbchina.cs.assitsvc.domain.CopilotConfigSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Copilot 独立配置缓存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CopilotConfigCache {

    private final CopilotConfigRepository configRepository;
    private final CopilotConfigValidationService validationService;

    private volatile CopilotConfigSnapshot currentSnapshot;
    private volatile Instant lastLoadTime;

    /**
     * 启动时尝试加载最新 Copilot 配置。加载失败不阻断服务启动，运行时返回空候选。
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
     * 重新加载最新 Copilot 配置。
     */
    public synchronized void loadLatestVersion() {
        CopilotConfigSnapshot snapshot = configRepository.loadLatestSnapshot();
        ConfigValidationResult validationResult = validationService.validate(snapshot);
        if (!validationResult.isSuccess()) {
            throw new IllegalStateException("Copilot config validation failed: " + validationResult.getErrors());
        }
        currentSnapshot = snapshot;
        lastLoadTime = Instant.now();
        log.info("[M07] Loaded copilot config, version={}, loadTime={}", snapshot.getVersionId(), lastLoadTime);
    }

    /**
     * 按意图代码查找候选动作引用。
     *
     * @param intentCode 意图代码
     * @return 候选动作引用列表
     */
    public List<ActionReference> findCandidatesByIntent(String intentCode) {
        CopilotConfigSnapshot snapshot = currentSnapshot;
        if (snapshot == null) {
            return Collections.emptyList();
        }

        Map<String, List<ActionReference>> intentToActions = snapshot.getIntentToActions();
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
        CopilotConfigSnapshot snapshot = currentSnapshot;
        if (snapshot == null) {
            return null;
        }

        Map<String, CopilotActionConfig> actionById = snapshot.getActionById();
        return actionById == null ? null : actionById.get(actionId);
    }

    /**
     * 获取当前 Copilot 配置版本号。
     *
     * @return 版本号；未加载时返回 null
     */
    public String getCurrentVersion() {
        CopilotConfigSnapshot snapshot = currentSnapshot;
        return snapshot == null ? null : snapshot.getVersionId();
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
        CopilotConfigSnapshot snapshot = currentSnapshot;
        if (snapshot == null) {
            return 0;
        }
        if (snapshot.getIntentToActions() == null) {
            return 0;
        }
        return snapshot.getIntentToActions().size();
    }

    /**
     * 统计当前启用 Copilot 的动作数量。
     *
     * @return 动作数量
     */
    public int getCopilotEnabledActionCount() {
        CopilotConfigSnapshot snapshot = currentSnapshot;
        if (snapshot == null) {
            return 0;
        }
        Map<String, CopilotActionConfig> actionById = snapshot.getActionById();
        return actionById == null ? 0 : actionById.size();
    }
}
