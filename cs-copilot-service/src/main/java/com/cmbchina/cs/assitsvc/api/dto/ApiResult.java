package com.cmbchina.cs.assitsvc.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用 API 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult {

    private String code;
    private String message;
    private String details;

    public static ApiResult ok() {
        return ApiResult.builder().code("0000").message("OK").build();
    }

    public static ApiResult fail(String code, String message) {
        return ApiResult.builder().code(code).message(message).build();
    }

    public static ApiResult fail(String code, String message, String details) {
        return ApiResult.builder().code(code).message(message).details(details).build();
    }
}
