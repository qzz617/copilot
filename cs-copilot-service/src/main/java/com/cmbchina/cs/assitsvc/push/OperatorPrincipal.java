package com.cmbchina.cs.assitsvc.push;

import java.security.Principal;

/**
 * WebSocket 坐席 Principal，name 固定为 operatorId。
 */
public class OperatorPrincipal implements Principal {

    private final String name;

    public OperatorPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
