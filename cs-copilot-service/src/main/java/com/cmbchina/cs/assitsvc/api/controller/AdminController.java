package com.cmbchina.cs.assitsvc.api.controller;

import com.cmbchina.cs.assitsvc.api.dto.ApiResult;
import com.cmbchina.cs.assitsvc.api.dto.AdminResult;
import com.cmbchina.cs.assitsvc.config.ConfigValidationResult;
import com.cmbchina.cs.assitsvc.config.CopilotConfigCache;
import com.cmbchina.cs.assitsvc.config.CopilotConfigValidationService;
import com.cmbchina.cs.assitsvc.core.intent.IntentTreeLoader;
import com.cmbchina.cs.assitsvc.domain.MenuVersionData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Copilot 管理接口。
 */
@RestController
@RequestMapping("/copilot/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IntentTreeLoader intentTreeLoader;
    private final CopilotConfigCache configCache;
    private final CopilotConfigValidationService validationService;

    /**
     * 重新加载当前 Pod 的意图树配置。
     *
     * @return 加载结果
     */
    @PostMapping("/intent-tree/reload")
    public AdminResult reloadIntentTree() {
        intentTreeLoader.reload();
        return AdminResult.builder()
                .code("0000")
                .message("OK")
                .currentVersion(intentTreeLoader.getVersion())
                .nodeCount(intentTreeLoader.getNodeCount())
                .reloadTime(intentTreeLoader.getLastLoadTime().toString())
                .build();
    }

    /**
     * 重新加载当前 Pod 的 CLOB 配置。
     *
     * @return 加载结果
     */
    @PostMapping("/config/refresh")
    public AdminResult refreshConfig() {
        long begin = System.currentTimeMillis();
        configCache.loadLatestVersion();
        return AdminResult.builder()
                .code("0000")
                .message("OK")
                .currentVersion(configCache.getCurrentVersion())
                .loadTimeMs(System.currentTimeMillis() - begin)
                .intentMappingCount(configCache.getIntentMappingCount())
                .copilotEnabledItemCount(configCache.getCopilotEnabledItemCount())
                .reloadTime(configCache.getLastLoadTime() == null ? null : configCache.getLastLoadTime().toString())
                .build();
    }

    /**
     * 校验待发布的 Copilot CLOB 配置。
     *
     * @param data 待校验配置
     * @return 校验结果
     */
    @PostMapping("/config/validate")
    public ApiResult validateConfig(@RequestBody MenuVersionData data) {
        ConfigValidationResult result = validationService.validate(data);
        if (result.isSuccess()) {
            return ApiResult.ok();
        }
        return ApiResult.fail("4000", "CONFIG_VALIDATION_FAILED", String.valueOf(result.getErrors()));
    }
}
