package com.cmbchina.cs.assitsvc.api.controller;

import com.cmbchina.cs.assitsvc.api.dto.CopilotHealthResult;
import com.cmbchina.cs.assitsvc.config.CopilotConfigCache;
import com.cmbchina.cs.assitsvc.config.CopilotConfigRepository;
import com.cmbchina.cs.assitsvc.core.intent.IntentTreeLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private final StringRedisTemplate redisTemplate;
    private final CopilotConfigRepository configRepository;

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
        try {
            String pong = redisTemplate.execute(
                    (RedisCallback<String>) connection -> connection.ping());
            return "PONG".equals(pong) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkDb() {
        try {
            configRepository.fetchLatestVersionMarker();
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
