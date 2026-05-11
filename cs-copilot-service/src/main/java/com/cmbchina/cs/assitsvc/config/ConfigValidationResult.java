package com.cmbchina.cs.assitsvc.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Copilot 配置校验结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigValidationResult {

    private boolean success;
    private List<String> errors;

    public static ConfigValidationResult ok() {
        return ConfigValidationResult.builder().success(true).errors(new ArrayList<String>()).build();
    }

    public static ConfigValidationResult fail(List<String> errors) {
        return ConfigValidationResult.builder().success(false).errors(errors).build();
    }
}
