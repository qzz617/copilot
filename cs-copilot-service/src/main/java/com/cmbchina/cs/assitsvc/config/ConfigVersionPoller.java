package com.cmbchina.cs.assitsvc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 多 Pod 配置一致性轮询器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigVersionPoller {

    private final CopilotConfigRepository configRepository;
    private final CopilotConfigCache configCache;

    /**
     * 定时检查最新配置版本，发现变化后刷新本地缓存。
     */
    @Scheduled(fixedDelayString = "${copilot.config-refresh.polling-interval-ms:30000}")
    public void poll() {
        try {
            String latestVersion = configRepository.fetchLatestVersionMarker();
            String currentVersion = configCache.getCurrentVersion();
            if (!StringUtils.hasText(latestVersion) || latestVersion.equals(currentVersion)) {
                return;
            }
            log.info("[M17] Config version changed, currentVersion={}, latestVersion={}",
                    currentVersion, latestVersion);
            configCache.loadLatestVersion();
        } catch (Exception e) {
            log.warn("[M17] Config version polling failed", e);
        }
    }
}
