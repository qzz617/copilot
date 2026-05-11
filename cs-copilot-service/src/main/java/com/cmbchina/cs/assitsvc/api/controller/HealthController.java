package com.cmbchina.cs.assitsvc.api.controller;

import com.cmbchina.cs.assitsvc.api.dto.CopilotHealthResult;
import com.cmbchina.cs.assitsvc.config.CopilotConfigCache;
import com.cmbchina.cs.assitsvc.config.MenuVersionDao;
import com.cmbchina.cs.assitsvc.core.intent.IntentTreeLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Copilot 健康检查接口。
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final long startTime = System.currentTimeMillis();

    private final CopilotConfigCache configCache;
    private final IntentTreeLoader intentTreeLoader;
    private final JedisPool jedisPool;
    private final MenuVersionDao menuVersionDao;

    /**
     * 健康检查。
     *
     * @return 健康检查结果
     */
    @GetMapping("/copilot/health")
    public CopilotHealthResult health() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("redis", checkRedis());
        details.put("db", checkDb());
        details.put("config", configCache.getCurrentVersion() == null ? "DOWN" : "UP");
        details.put("intentTree", intentTreeLoader.getTree() == null ? "DOWN" : "UP");

        String status = details.containsValue("DOWN") ? "DOWN" : "UP";
        return CopilotHealthResult.builder()
                .status(status)
                .configVersion(configCache.getCurrentVersion())
                .intentTreeVersion(intentTreeLoader.getVersion())
                .uptime((System.currentTimeMillis() - startTime) / 1000)
                .details(details)
                .build();
    }

    private String checkRedis() {
        try (Jedis jedis = jedisPool.getResource()) {
            return "PONG".equals(jedis.ping()) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkDb() {
        try {
            menuVersionDao.fetchLatestVersionMarker();
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
