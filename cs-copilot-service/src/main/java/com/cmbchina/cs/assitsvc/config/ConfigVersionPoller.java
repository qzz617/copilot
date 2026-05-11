package com.cmbchina.cs.assitsvc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.SetParams;

/**
 * 多 Pod 配置一致性轮询器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigVersionPoller {

    private static final String LOCK_KEY_PREFIX = "copilot:config_refresh_lock:";

    private final MenuVersionDao menuVersionDao;
    private final CopilotConfigCache configCache;
    private final JedisPool jedisPool;

    @Value("${copilot.config-refresh.lock-ttl-seconds:30}")
    private int lockTtlSeconds;

    /**
     * 定时检查最新配置版本，发现变化后刷新本地缓存。
     */
    @Scheduled(fixedDelayString = "${copilot.config-refresh.polling-interval-ms:30000}")
    public void poll() {
        try {
            String latestVersion = menuVersionDao.fetchLatestVersionMarker();
            String currentVersion = configCache.getCurrentVersion();
            if (!StringUtils.hasText(latestVersion) || latestVersion.equals(currentVersion)) {
                return;
            }
            log.info("[M17] Config version changed, currentVersion={}, latestVersion={}",
                    currentVersion, latestVersion);
            if (!tryAcquireRefreshLock(latestVersion)) {
                log.debug("[M17] Config refresh skipped by cluster lock, latestVersion={}", latestVersion);
                return;
            }
            configCache.loadLatestVersion();
        } catch (Exception e) {
            log.warn("[M17] Config version polling failed", e);
        }
    }

    private boolean tryAcquireRefreshLock(String latestVersion) {
        String lockKey = LOCK_KEY_PREFIX + latestVersion;
        int ttlSeconds = Math.max(1, lockTtlSeconds);
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.set(lockKey, "1", new SetParams().nx().ex(ttlSeconds));
            return "OK".equals(result);
        } catch (JedisException e) {
            log.warn("[M17] Config refresh cluster lock unavailable, fallback to local refresh", e);
            return true;
        }
    }
}
