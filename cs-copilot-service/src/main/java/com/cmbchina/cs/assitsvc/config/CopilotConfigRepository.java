package com.cmbchina.cs.assitsvc.config;

import com.cmbchina.cs.assitsvc.domain.CopilotConfigSnapshot;

/**
 * Copilot 独立配置读取仓储。
 */
public interface CopilotConfigRepository {

    /**
     * 查询最新已发布版本标识，用于多 Pod 轮询判断是否需要刷新。
     *
     * @return 版本标识；不存在时返回 null
     */
    String fetchLatestVersionMarker();

    /**
     * 从配置表加载最新已发布配置并构建运行时快照。
     *
     * @return 运行时配置快照
     */
    CopilotConfigSnapshot loadLatestSnapshot();
}
