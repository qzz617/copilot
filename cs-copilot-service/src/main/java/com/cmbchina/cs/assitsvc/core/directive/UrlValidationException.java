package com.cmbchina.cs.assitsvc.core.directive;

/**
 * URL 校验异常。
 */
public class UrlValidationException extends RuntimeException {

    public UrlValidationException(String message) {
        super(message);
    }

    public UrlValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
