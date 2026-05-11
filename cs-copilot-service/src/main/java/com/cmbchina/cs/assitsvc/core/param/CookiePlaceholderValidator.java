package com.cmbchina.cs.assitsvc.core.param;

import com.cmbchina.cs.assitsvc.domain.ItemParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Cookie 占位符白名单与域名绑定校验器。
 */
@Component
@RequiredArgsConstructor
public class CookiePlaceholderValidator {

    private static final Pattern COOKIE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_.-]+");

    private final CookiePlaceholderProperties props;

    /**
     * 校验 Cookie 占位符配置。
     *
     * @param param     参数配置
     * @param targetUrl 目标 URL
     * @return true 表示允许使用
     */
    public boolean validate(ItemParam param, String targetUrl) {
        if (param == null || !"COOKIE_PLACEHOLDER".equals(param.getParamType())) {
            return true;
        }
        if (!props.isEnabled()) {
            return false;
        }

        String cookieName = param.getParamValue();
        if (!StringUtils.hasText(cookieName) || !COOKIE_NAME_PATTERN.matcher(cookieName).matches()) {
            return false;
        }
        if (!props.getAllowedCookies().contains(cookieName)) {
            return false;
        }

        String domain = extractDomain(targetUrl);
        if (!StringUtils.hasText(domain)) {
            return false;
        }

        List<String> allowedForDomain = props.getDomainBindings().get(domain);
        return allowedForDomain != null && allowedForDomain.contains(cookieName);
    }

    private static String extractDomain(String targetUrl) {
        if (!StringUtils.hasText(targetUrl)) {
            return null;
        }
        try {
            return new URI(targetUrl).getHost();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
