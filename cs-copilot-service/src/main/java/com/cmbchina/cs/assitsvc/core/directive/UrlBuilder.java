package com.cmbchina.cs.assitsvc.core.directive;

import java.util.Map;

/**
 * 跳转 URL 构建器。
 */
public interface UrlBuilder {

    /**
     * 构建安全跳转 URL。
     *
     * @param baseUrl 基础 URL
     * @param params  参数
     * @return 完整 URL
     */
    String buildUrl(String baseUrl, Map<String, String> params);
}
