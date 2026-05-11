package com.cmbchina.cs.assitsvc.api;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 管理接口鉴权配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "copilot.admin.auth")
public class AdminSecurityProperties {

    /** 是否启用管理接口 token 校验。 */
    private boolean enabled = true;

    /** 管理接口请求头名称。 */
    private String headerName = "X-Copilot-Admin-Token";

    /** 管理接口 token。为空时拒绝所有管理请求。 */
    private String token;
}
