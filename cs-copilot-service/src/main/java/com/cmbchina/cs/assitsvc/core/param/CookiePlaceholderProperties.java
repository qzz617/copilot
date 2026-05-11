package com.cmbchina.cs.assitsvc.core.param;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cookie 占位符安全配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "copilot.cookie-placeholder")
public class CookiePlaceholderProperties {

    /** 是否启用 Cookie 占位符。 */
    private boolean enabled = true;

    /** Cookie 名白名单。 */
    private List<String> allowedCookies = new ArrayList<>();

    /** 目标域名与 Cookie 名绑定关系。 */
    private Map<String, List<String>> domainBindings = new HashMap<>();

    /** 前端必填 Cookie 列表。 */
    private List<String> requiredCookies = new ArrayList<>();
}
