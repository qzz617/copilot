package com.cmbchina.cs.assitsvc.infra.metrics;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * MVP 阶段反馈结果 ES 写入客户端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackEsClient {

    private final FeedbackEsProperties properties;

    /**
     * 写入反馈结果到 ES。失败抛出异常，由调用方决定是否阻塞。
     *
     * @param record 反馈记录
     */
    public void index(FeedbackLogRecord record) {
        if (record == null || !properties.isEnabled()) {
            return;
        }
        String url = buildDocumentUrl(record.getLogId());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(properties.getUsername())) {
            headers.setBasicAuth(properties.getUsername(),
                    properties.getPassword() == null ? "" : properties.getPassword());
        }
        buildRestTemplate().exchange(url, HttpMethod.PUT,
                new HttpEntity<String>(JSON.toJSONString(record), headers), String.class);
        log.debug("[M16] Feedback result indexed to ES, logId={}", record.getLogId());
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        return new RestTemplate(factory);
    }

    private String buildDocumentUrl(String logId) {
        String baseUrl = trimTrailingSlash(properties.getBaseUrl());
        return baseUrl + "/" + properties.getIndexName() + "/_doc/" + logId;
    }

    private static String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
