package com.cmbchina.cs.assitsvc.config;

import com.cmbchina.cs.assitsvc.domain.CopilotConfigSnapshot;

/**
 * Copilot 配置基础校验服务。
 */
public interface CopilotConfigValidationService {

    /**
     * 校验 Copilot 配置快照。
     *
     * @param snapshot 配置快照
     * @return 校验结果
     */
    ConfigValidationResult validate(CopilotConfigSnapshot snapshot);
}
