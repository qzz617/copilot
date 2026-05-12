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
 * cs_copilot_feedback_log 记录。
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
    @TableField("item_id")
    private Long itemId;
    @TableField("frontend_reason")
    private String frontendReason;
    @TableField("is_effective")
    private String isEffective;
    @TableField("feedback_time")
    private Instant feedbackTime;
}
