package com.cmbchina.cs.assitsvc.core.param;

/**
 * 前端工作台可取值的标准业务参数类型。
 */
public enum StandardParamType {

    CUST_NO("客户号", false),
    CUST_ID_NO("证件号", true),
    MOBPHN1("预留手机号", true),
    ACCOUNT_NO("账户号", false);

    private final String displayName;
    private final boolean sensitive;

    StandardParamType(String displayName, boolean sensitive) {
        this.displayName = displayName;
        this.sensitive = sensitive;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSensitive() {
        return sensitive;
    }
}
