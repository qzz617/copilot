package com.cmbchina.cs.assitsvc.infra.metrics;

import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 反馈结果 ES High Level REST Client 配置。
 */
@Configuration
@RequiredArgsConstructor
public class FeedbackEsClientConfig {

    private final FeedbackEsProperties properties;

    /**
     * 构建反馈结果写入 ES 使用的 High Level REST Client。
     *
     * @return ES High Level REST Client
     */
    @Bean(name = "feedbackRestHighLevelClient", destroyMethod = "close")
    @ConditionalOnProperty(prefix = "copilot.metrics.feedback-es", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public RestHighLevelClient feedbackRestHighLevelClient() {
        RestClientBuilder builder = RestClient.builder(parseHosts(properties.getBaseUrl()))
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(properties.getConnectTimeoutMs())
                        .setSocketTimeout(properties.getReadTimeoutMs()));

        if (StringUtils.hasText(properties.getUsername())) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(properties.getUsername(), safePassword()));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        return new RestHighLevelClient(builder);
    }

    private HttpHost[] parseHosts(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalArgumentException("copilot.metrics.feedback-es.base-url must not be empty");
        }
        String[] values = baseUrl.split(",");
        List<HttpHost> hosts = new ArrayList<>();
        for (String value : values) {
            String host = value == null ? "" : value.trim();
            if (StringUtils.hasText(host)) {
                hosts.add(HttpHost.create(host));
            }
        }
        if (hosts.isEmpty()) {
            throw new IllegalArgumentException("copilot.metrics.feedback-es.base-url must contain valid ES hosts");
        }
        return hosts.toArray(new HttpHost[hosts.size()]);
    }

    private String safePassword() {
        return properties.getPassword() == null ? "" : properties.getPassword();
    }
}
