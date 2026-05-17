package com.cmbchina.cs.assitsvc.infra.metrics;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 反馈结果 ES 写入配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "copilot.metrics.feedback-es")
public class FeedbackEsProperties {

    /** 是否启用反馈结果写入 ES。 */
    private boolean enabled = true;

    /** ES 基础地址，例如 http://localhost:9200。 */
    private String baseUrl = "http://localhost:9200";

    /** 反馈结果索引名。 */
    private String indexName = "cs-copilot-feedback-log";

    /** ES 用户名，可为空。 */
    private String username;

    /** ES 密码，可为空。 */
    private String password;

    /** 连接超时时间，毫秒。 */
    private int connectTimeoutMs = 1000;

    /** 读取超时时间，毫秒。 */
    private int readTimeoutMs = 1000;
}
