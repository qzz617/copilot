package com.cmbchina.cs.assitsvc.config;

import com.cmbchina.cs.assitsvc.domain.MenuVersionData;

/**
 * Copilot CLOB 基础校验服务。
 */
public interface CopilotConfigValidationService {

    /**
     * 校验 CLOB 配置。
     *
     * @param data CLOB 数据
     * @return 校验结果
     */
    ConfigValidationResult validate(MenuVersionData data);
}
