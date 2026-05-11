package com.cmbchina.cs.assitsvc.core.match;

import com.cmbchina.cs.assitsvc.domain.ConditionRule;
import com.cmbchina.cs.assitsvc.domain.EvaluationContext;

/**
 * 条件规则评估器。
 */
public interface RuleEvaluator {

    /**
     * 评估条件规则。
     *
     * @param rule 条件规则，null 表示通过
     * @param ctx  评估上下文
     * @return true 表示通过
     */
    boolean evaluate(ConditionRule rule, EvaluationContext ctx);

    /**
     * 评估 JSON 格式条件规则。
     *
     * @param ruleJson 条件规则 JSON，空表示通过
     * @param ctx      评估上下文
     * @return true 表示通过
     */
    boolean evaluate(String ruleJson, EvaluationContext ctx);
}
