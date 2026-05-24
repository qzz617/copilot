package com.cmbchina.cs.assitsvc.asr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 意图识别触发窗口配置（MVP 简化方案）。
 *
 * <p>策略：同一 callId 收到第一条客户句子后开启固定窗口（{@link #windowMs}），
 * 窗口期内的客户句子仅追加到对话历史、不重置窗口；窗口到期触发一次意图识别。
 */
@Data
@Component
@ConfigurationProperties(prefix = "copilot.intent-trigger")
public class IntentTriggerProperties {

    /** 固定窗口长度（ms），首条客户句子起算。 */
    private long windowMs = 5000L;

    /** Redis ZSET 延迟队列轮询间隔（ms）。 */
    private long pollingIntervalMs = 200L;

    /** 单次轮询的最大批量条数。 */
    private int pollingBatchSize = 100;

    /** fire 抛异常后，本通话进入冷却的时长（ms），期间不再开新窗口，避免对故障 AI 持续打。 */
    private long fireFailureCooldownMs = 30_000L;
}
