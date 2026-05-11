package com.cmbchina.cs.assitsvc.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 通话解绑请求。
 */
@Data
public class UnbindRequest {

    /** 通话 ID。 */
    @NotBlank
    private String callId;
}
