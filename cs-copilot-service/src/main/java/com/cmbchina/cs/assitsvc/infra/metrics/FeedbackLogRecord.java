package com.cmbchina.cs.assitsvc.infra.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 反馈日志记录，写入 ES。
 *
 * <p>字段命名沿用 snake_case 风格的字段名空间（ES 索引侧映射时由 fastjson 默认 camelCase 序列化）。
 * 如后期需要切换 DB 持久化或精确控制 ES 字段名，再补 Jackson/fastjson 注解。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackLogRecord {

    private String logId;
    private String directiveId;
    private String triggerLogId;
    private String callId;
    private String operatorId;
    private String feedbackType;
    private String intentCode;
    private String actionId;
    private Long menuItemId;
    private String isEffective;
    private Instant feedbackTime;
}
