package com.cmbchina.cs.assitsvc.infra.metrics;

/**
 * Copilot 业务原因码常量。
 */
public final class ReasonCodeConstants {

    /** @deprecated 统一使用 {@link #AI_NETWORK_FAIL}。 */
    @Deprecated
    public static final String AI_TIMEOUT = "AI_TIMEOUT";
    /** @deprecated 统一使用 {@link #AI_BUSINESS_FAIL}。 */
    @Deprecated
    public static final String AI_FAILED = "AI_FAILED";
    public static final String AI_CIRCUIT_BREAKER_OPEN = "AI_CIRCUIT_BREAKER_OPEN";
    public static final String AI_NETWORK_FAIL = "AI_NETWORK_FAIL";
    public static final String AI_BUSINESS_FAIL = "AI_BUSINESS_FAIL";
    public static final String NO_CUSTOMER_HISTORY = "NO_CUSTOMER_HISTORY";
    public static final String INTENT_EMPTY = "INTENT_EMPTY";
    public static final String INTENT_NOT_MAPPED = "INTENT_NOT_MAPPED";
    public static final String RISK_DISABLED = "RISK_DISABLED";
    public static final String URL_NOT_ALLOWED = "URL_NOT_ALLOWED";
    public static final String PARAM_TYPE_NOT_ALLOWED = "PARAM_TYPE_NOT_ALLOWED";
    public static final String PARAM_MISSING = "PARAM_MISSING";
    public static final String SESSION_BIND_MISSING = "SESSION_BIND_MISSING";
    public static final String GRAY_FILTERED = "GRAY_FILTERED";
    public static final String MUTED_BY_AGENT = "MUTED_BY_AGENT";
    public static final String DIRECTIVE_BUILD_ERROR = "DIRECTIVE_BUILD_ERROR";
    public static final String PUSH_FAILED = "PUSH_FAILED";

    private ReasonCodeConstants() {
    }
}
