package com.tchalanet.server.common.exception;

public class TchValidationException extends TchException {

    public TchValidationException(String code, String message) {
        super(code, message);
    }
}
