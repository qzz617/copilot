package com.cmbchina.cs.assitsvc.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin 接口通用结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminResult {

    /** 响应码，0000 表示成功。 */
    private String code;

    /** 响应消息。 */
    private String message;

    /** 当前配置版本。 */
    private String currentVersion;

    /** 意图树节点数。 */
    private Integer nodeCount;

    /** 重新加载时间，ISO-8601 字符串。 */
    private String reloadTime;

    /** 加载耗时，毫秒。 */
    private Long loadTimeMs;

    /** 意图映射数量。 */
    private Integer intentMappingCount;

    /** Copilot 启用功能数量。 */
    private Integer copilotEnabledItemCount;
}
