package com.cmbchina.cs.assitsvc.asr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ASR 事件接入配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "copilot.asr")
public class AsrProperties {

    /** Kafka topic 名称。 */
    private String topic = "cs.asr.sentences";

    /** Kafka listener 并发数。 */
    private int concurrency = 4;

    /** 触发意图识别的最小文本长度。 */
    private int minTextLength = 4;

    /** ASR 置信度阈值。 */
    private float asrConfidenceThreshold = 0.65F;

    /** confidence 缺失时是否允许通过触发过滤。 */
    private boolean asrConfidenceDefaultPass = true;

    /** sentenceId 去重 key 的保留时间。 */
    private int dedupTtlHours = 2;
}
