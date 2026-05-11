package com.cmbchina.cs.assitsvc.core.directive;

/**
 * 指令构建异常。
 */
public class DirectiveBuildException extends RuntimeException {

    public DirectiveBuildException(String message) {
        super(message);
    }

    public DirectiveBuildException(String message, Throwable cause) {
        super(message, cause);
    }
}
