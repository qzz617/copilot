package com.cmbchina.cs.assitsvc.core.param;

/**
 * 标准参数类型。
 */
public enum StandardParamType {

    CUST_NO("客户号", ParamSourceType.SESSION, "customer.customerId", false),
    CUST_ID_NO("证件号", ParamSourceType.SESSION, "customer.idNo", true),
    CUST_ID_NO_NOTYPE("无证件号类型", ParamSourceType.SESSION, "customer.noIdType", false),
    PALM_LIFE_USER_ID("掌上生活用户ID", ParamSourceType.SESSION, "customer.palmLifeUserId", false),
    MOBPHN1("预留手机号一", ParamSourceType.SESSION, "customer.phoneNo", true),
    MOBPHN1_NO_ZERO("预留手机号一(去0)", ParamSourceType.SESSION, "customer.phoneNoNoZero", true),
    ACCOUNT_NO("账户号", ParamSourceType.SESSION, "accounts[0].accountNo", false),
    ADDR_TEXT("地址", ParamSourceType.SESSION, "customer.address", true),
    ENCODE_ADDR_TEXT("地址(编码)", ParamSourceType.SESSION, "customer.addressEncode", true),
    CALL_ID("通话ID", ParamSourceType.CALL_META, "callId", false),
    IN_LINE_N0("进线号码", ParamSourceType.CALL_META, "calledNumber", true),
    SUPP_CARD_INTERCEPT("是否拦截纯附属卡人", ParamSourceType.LITERAL, null, false),
    EXP("自定义参数", ParamSourceType.LITERAL, null, false),
    COOKIE_PLACEHOLDER("Cookie 占位符", ParamSourceType.COOKIE, null, false);

    private final String displayName;
    private final ParamSourceType sourceType;
    private final String defaultSourceKey;
    private final boolean sensitive;

    StandardParamType(String displayName, ParamSourceType sourceType, String defaultSourceKey, boolean sensitive) {
        this.displayName = displayName;
        this.sourceType = sourceType;
        this.defaultSourceKey = defaultSourceKey;
        this.sensitive = sensitive;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ParamSourceType getSourceType() {
        return sourceType;
    }

    public String getDefaultSourceKey() {
        return defaultSourceKey;
    }

    public boolean isSensitive() {
        return sensitive;
    }
}
