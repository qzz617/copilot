package com.cmbchina.cs.assitsvc.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 条件项，condition_rule 中的单条规则。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionItem {

    /** 字段路径，如 customer.customerType */
    private String field;

    /** 运算符：eq / not_eq / in / not_in / exists / not_exists / gt / gte / lt / lte */
    private String op;

    /** 期望值，类型因 op 而异（String / Number / List） */
    private Object value;
}
