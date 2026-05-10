package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 评估上下文，供 RuleEvaluator 进行条件规则计算时取值。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationContext {

    /** 通话 ID */
    private String callId;

    /** 坐席工号 */
    private String operatorId;

    /** 客户号 */
    private String customerId;

    /** 客户类型，如 VIP3 / Normal */
    private String customerType;

    /** 客户 session 数据（key 为字段路径，支持嵌套路径查找） */
    private Map<String, Object> sessionData;

    /** 通话元数据（callId、calledNumber 等） */
    private Map<String, Object> callMetaData;
}
