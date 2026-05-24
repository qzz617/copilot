package com.cmbchina.cs.assitsvc.api.controller;

import com.cmbchina.cs.assitsvc.api.AdminSecurityProperties;
import com.cmbchina.cs.assitsvc.api.dto.ApiResult;
import com.cmbchina.cs.assitsvc.api.dto.AdminResult;
import com.cmbchina.cs.assitsvc.config.ConfigValidationResult;
import com.cmbchina.cs.assitsvc.config.CopilotConfigCache;
import com.cmbchina.cs.assitsvc.config.CopilotConfigRepository;
import com.cmbchina.cs.assitsvc.config.CopilotConfigValidationService;
import com.cmbchina.cs.assitsvc.core.intent.IntentTreeLoader;
import com.cmbchina.cs.assitsvc.domain.CopilotConfigSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

/**
 * Copilot 管理接口。
 */
@RestController
@RequestMapping("/copilot/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IntentTreeLoader intentTreeLoader;
    private final CopilotConfigCache configCache;
    private final CopilotConfigRepository configRepository;
    private final CopilotConfigValidationService validationService;
    private final AdminSecurityProperties securityProperties;

    /**
     * 重新加载当前 Pod 的意图树配置。
     *
     * @return 加载结果
     */
    @PostMapping("/intent-tree/reload")
    public AdminResult reloadIntentTree(HttpServletRequest request) {
        authorize(request);
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
     * 重新加载当前 Pod 的 Copilot 配置。
     *
     * @return 加载结果
     */
    @PostMapping("/config/refresh")
    public AdminResult refreshConfig(HttpServletRequest request) {
        authorize(request);
        long begin = System.currentTimeMillis();
        configCache.loadLatestVersion();
        return AdminResult.builder()
                .code("0000")
                .message("OK")
                .currentVersion(configCache.getCurrentVersion())
                .loadTimeMs(System.currentTimeMillis() - begin)
                .intentMappingCount(configCache.getIntentMappingCount())
                .copilotEnabledActionCount(configCache.getCopilotEnabledActionCount())
                .reloadTime(configCache.getLastLoadTime() == null ? null : configCache.getLastLoadTime().toString())
                .build();
    }

    /**
     * 校验待发布的 Copilot 配置。
     *
     * @param versionId 待校验配置版本号；传入时校验数据库快照
     * @param data      待校验内存快照；仅用于本地调试兼容
     * @return 校验结果
     */
    @PostMapping("/config/validate")
    public ApiResult validateConfig(HttpServletRequest request,
                                    @RequestParam(value = "versionId", required = false) String versionId,
                                    @RequestBody(required = false) CopilotConfigSnapshot data) {
        authorize(request);
        try {
            CopilotConfigSnapshot snapshot = resolveValidationSnapshot(versionId, data);
            ConfigValidationResult result = validationService.validate(snapshot);
            if (result.isSuccess()) {
                return ApiResult.ok();
            }
            return ApiResult.fail("4000", "CONFIG_VALIDATION_FAILED", String.valueOf(result.getErrors()));
        } catch (RuntimeException e) {
            return ApiResult.fail("4000", "CONFIG_VALIDATION_FAILED", e.getMessage());
        }
    }

    private CopilotConfigSnapshot resolveValidationSnapshot(String versionId, CopilotConfigSnapshot data) {
        if (StringUtils.hasText(versionId)) {
            return configRepository.loadSnapshot(versionId);
        }
        if (data != null) {
            return data;
        }
        return configRepository.loadLatestSnapshot();
    }

    private void authorize(HttpServletRequest request) {
        if (!securityProperties.isEnabled()) {
            return;
        }
        String actualToken = request.getHeader(securityProperties.getHeaderName());
        String expectedToken = securityProperties.getToken();
        if (StringUtils.hasText(expectedToken) && expectedToken.equals(actualToken)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "admin token invalid");
    }
}
