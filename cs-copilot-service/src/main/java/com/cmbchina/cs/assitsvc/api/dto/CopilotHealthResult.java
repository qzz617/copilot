package com.cmbchina.cs.assitsvc.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Copilot 健康检查结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopilotHealthResult {

    private String status;
    private String configVersion;
    private String intentTreeVersion;
    private Long uptime;
    private Map<String, String> details;
}
