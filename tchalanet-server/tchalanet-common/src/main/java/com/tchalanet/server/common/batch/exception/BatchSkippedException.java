package com.tchalanet.server.common.batch.exception;

public class BatchSkippedException extends RuntimeException {
    private final String code;

    public BatchSkippedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
