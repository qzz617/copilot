package com.cmbchina.cs.assitsvc.core.match;

import com.alibaba.fastjson2.JSON;
import com.cmbchina.cs.assitsvc.domain.ConditionItem;
import com.cmbchina.cs.assitsvc.domain.ConditionRule;
import com.cmbchina.cs.assitsvc.domain.EvaluationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * JSON condition_rule 评估器。
 */
@Slf4j
@Component
public class JsonRuleEvaluatorImpl implements RuleEvaluator {

    @Override
    public boolean evaluate(String ruleJson, EvaluationContext ctx) {
        if (!StringUtils.hasText(ruleJson)) {
            return true;
        }
        try {
            return evaluate(JSON.parseObject(ruleJson, ConditionRule.class), ctx);
        } catch (Exception e) {
            log.error("[M07] Parse condition rule failed, ruleJson={}", ruleJson, e);
            return false;
        }
    }

    @Override
    public boolean evaluate(ConditionRule rule, EvaluationContext ctx) {
        if (rule == null) {
            return true;
        }
        try {
            return doEvaluate(rule, ctx);
        } catch (Exception e) {
            log.error("[M07] Evaluate condition rule failed, rule={}", JSON.toJSONString(rule), e);
            return false;
        }
    }

    private boolean doEvaluate(ConditionRule rule, EvaluationContext ctx) {
        if (rule.getAll() != null) {
            for (ConditionItem item : rule.getAll()) {
                if (!evaluateItem(item, ctx)) {
                    return false;
                }
            }
        }
        if (rule.getAny() != null && !rule.getAny().isEmpty()) {
            for (ConditionItem item : rule.getAny()) {
                if (evaluateItem(item, ctx)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private boolean evaluateItem(ConditionItem item, EvaluationContext ctx) {
        if (item == null || !StringUtils.hasText(item.getOp())) {
            return false;
        }

        Object actualValue = getValueByPath(ctx, item.getField());
        Object expectedValue = item.getValue();
        String op = item.getOp();

        if ("eq".equals(op)) {
            return Objects.equals(toComparableString(actualValue), toComparableString(expectedValue));
        }
        if ("not_eq".equals(op)) {
            return !Objects.equals(toComparableString(actualValue), toComparableString(expectedValue));
        }
        if ("in".equals(op)) {
            return contains(expectedValue, actualValue);
        }
        if ("not_in".equals(op)) {
            return !contains(expectedValue, actualValue);
        }
        if ("exists".equals(op)) {
            return actualValue != null;
        }
        if ("not_exists".equals(op)) {
            return actualValue == null;
        }
        if ("gt".equals(op) || "gte".equals(op) || "lt".equals(op) || "lte".equals(op)) {
            return compare(op, actualValue, expectedValue);
        }

        throw new IllegalArgumentException("Unknown condition op: " + op);
    }

    private static Object getValueByPath(EvaluationContext ctx, String path) {
        if (ctx == null || !StringUtils.hasText(path)) {
            return null;
        }
        if ("callId".equals(path)) {
            return ctx.getCallId();
        }
        if ("operatorId".equals(path)) {
            return ctx.getOperatorId();
        }
        if ("customerId".equals(path) || "customer.customerId".equals(path)) {
            return firstNonNull(ctx.getCustomerId(), lookup(ctx.getSessionData(), path));
        }
        if ("customerType".equals(path) || "customer.customerType".equals(path)) {
            return firstNonNull(ctx.getCustomerType(), lookup(ctx.getSessionData(), path));
        }

        Object sessionValue = lookup(ctx.getSessionData(), path);
        if (sessionValue != null) {
            return sessionValue;
        }
        return lookup(ctx.getCallMetaData(), path);
    }

    private static Object lookup(Map<String, Object> source, String path) {
        if (source == null || !StringUtils.hasText(path)) {
            return null;
        }
        if (source.containsKey(path)) {
            return source.get(path);
        }

        Object current = source;
        String[] parts = path.split("\\.");
        for (String part : parts) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(part);
        }
        return current;
    }

    private static Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private static boolean contains(Object expectedValue, Object actualValue) {
        if (expectedValue instanceof Collection) {
            String actual = toComparableString(actualValue);
            for (Object expected : (Collection<?>) expectedValue) {
                if (Objects.equals(toComparableString(expected), actual)) {
                    return true;
                }
            }
            return false;
        }
        if (expectedValue instanceof String) {
            String actual = toComparableString(actualValue);
            String[] values = ((String) expectedValue).split(",");
            for (String value : values) {
                if (Objects.equals(value.trim(), actual)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean compare(String op, Object actualValue, Object expectedValue) {
        BigDecimal actual = toBigDecimal(actualValue);
        BigDecimal expected = toBigDecimal(expectedValue);
        int compared = actual.compareTo(expected);
        if ("gt".equals(op)) {
            return compared > 0;
        }
        if ("gte".equals(op)) {
            return compared >= 0;
        }
        if ("lt".equals(op)) {
            return compared < 0;
        }
        return compared <= 0;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot compare null value");
        }
        return new BigDecimal(String.valueOf(value));
    }

    private static String toComparableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
