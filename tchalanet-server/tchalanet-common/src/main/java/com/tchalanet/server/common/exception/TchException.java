package com.tchalanet.server.common.exception;

public abstract class TchException extends RuntimeException {

    private final String code;

    protected TchException(String code, String message) {
        super(message);
        this.code = code;
    }

    protected TchException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
