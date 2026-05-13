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
 * cs_copilot_trigger_log 记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("svccfg.cs_copilot_trigger_log")
public class TriggerLogRecord {

    @TableId(value = "log_id", type = IdType.INPUT)
    private String logId;
    @TableField("call_id")
    private String callId;
    @TableField("operator_id")
    private String operatorId;
    @TableField("customer_id")
    private String customerId;
    @TableField("intent_code")
    private String intentCode;
    @TableField("intent_name")
    private String intentName;
    @TableField("action_id")
    private String actionId;
    @TableField("action_name")
    private String actionName;
    @TableField("menu_item_id")
    private Long menuItemId;
    @TableField("candidate_count")
    private Integer candidateCount;
    @TableField("risk_level")
    private String riskLevel;
    @TableField("directive_id")
    private String directiveId;
    @TableField("expire_at")
    private Instant expireAt;
    @TableField("directive_status")
    private String directiveStatus;
    @TableField("result_status")
    private String resultStatus;
    @TableField("reason_code")
    private String reasonCode;
    @TableField("filter_stage")
    private String filterStage;
    @TableField("trigger_time")
    private Instant triggerTime;
    @TableField("config_version")
    private String configVersion;
}
