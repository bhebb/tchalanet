package com.tchalanet.server.common.batch.exception;

public class BatchPartialFailureException extends RuntimeException {
    private final String code;

    public BatchPartialFailureException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
