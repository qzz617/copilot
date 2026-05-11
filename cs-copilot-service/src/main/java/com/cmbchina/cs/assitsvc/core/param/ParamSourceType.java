package com.cmbchina.cs.assitsvc.core.param;

/**
 * 参数来源类型。
 */
public enum ParamSourceType {

    /** 来自客户 session 上下文。 */
    SESSION,

    /** 来自通话元数据。 */
    CALL_META,

    /** 来自配置字面值。 */
    LITERAL,

    /** Cookie 占位符，由前端替换。 */
    COOKIE
}
