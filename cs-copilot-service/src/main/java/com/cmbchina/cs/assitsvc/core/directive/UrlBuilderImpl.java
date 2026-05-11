package com.cmbchina.cs.assitsvc.core.directive;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * URL 构建器实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UrlBuilderImpl implements UrlBuilder {

    private final UrlSecurityProperties props;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public String buildUrl(String baseUrl, Map<String, String> params) {
        URI uri = parseAndValidate(baseUrl);
        LinkedHashMap<String, String> existingParams = parseRawQuery(uri.getRawQuery());
        LinkedHashMap<String, String> validParams = filterValidParams(params);

        if (validParams.isEmpty()) {
            return baseUrl;
        }

        mergeParams(existingParams, validParams);
        return rebuildUrl(uri, existingParams);
    }

    private URI parseAndValidate(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new UrlValidationException("baseUrl must not be empty");
        }

        URI uri;
        try {
            uri = new URI(baseUrl);
        } catch (URISyntaxException e) {
            throw new UrlValidationException("Invalid URL: " + baseUrl, e);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new UrlValidationException("Only https protocol allowed: " + baseUrl);
        }
        String domain = uri.getHost();
        if (!StringUtils.hasText(domain) || !props.getUrlWhitelist().contains(domain)) {
            throw new UrlValidationException("Domain not in whitelist: " + domain);
        }
        if ("prod".equalsIgnoreCase(activeProfile) && props.getUrlBuilder().getUatDomains().contains(domain)) {
            throw new UrlValidationException("UAT domain not allowed in PROD: " + domain);
        }
        return uri;
    }

    private void mergeParams(LinkedHashMap<String, String> existingParams, LinkedHashMap<String, String> validParams) {
        String policy = props.getUrlBuilder().getSameKeyPolicy();
        String normalizedPolicy = StringUtils.hasText(policy) ? policy.trim().toUpperCase() : "OVERRIDE";

        Iterator<Map.Entry<String, String>> iterator = validParams.entrySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next().getKey();
            if (!existingParams.containsKey(key)) {
                continue;
            }
            if ("ERROR".equals(normalizedPolicy)) {
                throw new UrlValidationException("Param key conflict: " + key);
            }
            if ("PRESERVE".equals(normalizedPolicy)) {
                log.warn("[M09] Param key conflict preserved, key={}", key);
                iterator.remove();
                continue;
            }
            if ("OVERRIDE".equals(normalizedPolicy)) {
                log.warn("[M09] Param key conflict overridden, key={}", key);
                existingParams.remove(key);
                continue;
            }
            throw new UrlValidationException("Unknown same-key-policy: " + policy);
        }

        existingParams.putAll(validParams);
    }

    private static LinkedHashMap<String, String> filterValidParams(Map<String, String> params) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (params == null || params.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry != null && StringUtils.hasText(entry.getKey()) && StringUtils.hasText(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private static LinkedHashMap<String, String> parseRawQuery(String rawQuery) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(rawQuery)) {
            return result;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            if (!StringUtils.hasText(pair)) {
                continue;
            }
            int index = pair.indexOf('=');
            String rawKey = index >= 0 ? pair.substring(0, index) : pair;
            String rawValue = index >= 0 ? pair.substring(index + 1) : "";
            result.put(decode(rawKey), decode(rawValue));
        }
        return result;
    }

    private static String rebuildUrl(URI uri, LinkedHashMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append(uri.getScheme()).append("://").append(uri.getRawAuthority());
        if (uri.getRawPath() != null) {
            sb.append(uri.getRawPath());
        }
        if (!params.isEmpty()) {
            sb.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    sb.append("&");
                }
                sb.append(encode(entry.getKey())).append("=").append(encodeValue(entry.getValue()));
                first = false;
            }
        }
        if (StringUtils.hasText(uri.getRawFragment())) {
            sb.append("#").append(uri.getRawFragment());
        }
        return sb.toString();
    }

    private static String encodeValue(String value) {
        if (value != null && value.startsWith("${COOKIE.")) {
            return value;
        }
        return encode(value);
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UrlValidationException("UTF-8 encoding unavailable", e);
        }
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UrlValidationException("UTF-8 encoding unavailable", e);
        }
    }
}
