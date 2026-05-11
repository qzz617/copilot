package com.cmbchina.cs.assitsvc.infra.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对话历史 Redis 存储参数，绑定 copilot.history.*。
 */
@Data
@ConfigurationProperties(prefix = "copilot.history")
public class HistoryProperties {

    /** Redis List 最大条数，超出时裁剪最旧的；对应 DD-V1.2 §10.2 MAX_HISTORY */
    private int maxSize = 50;

    /** Key 有效期（小时），每次 append 刷新；对应 DD-V1.2 §10.2 TTL */
    private int ttlHours = 1;
}
