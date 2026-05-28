package com.cmbchina.cs.assitsvc.infra.metrics;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("svccfg.cs_copilot_feedback_log")
public class FeedbackLogRecord {

    @TableId(value = "log_id", type = IdType.INPUT)
    private String logId;
    @TableField("directive_id")
    private String directiveId;
    @TableField("trigger_log_id")
    private String triggerLogId;
    @TableField("call_id")
    private String callId;
    @TableField("operator_id")
    private String operatorId;
    @TableField("feedback_type")
    private String feedbackType;
    @TableField("intent_code")
    private String intentCode;
    private String actionId;
    private Long menuItemId;
    private String isEffective;
    @TableField("feedback_time")
    private Instant feedbackTime;
}
