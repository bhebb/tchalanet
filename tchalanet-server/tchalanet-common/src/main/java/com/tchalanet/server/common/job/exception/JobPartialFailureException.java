package com.tchalanet.server.common.job.exception;

public class JobPartialFailureException extends RuntimeException {
    private final String code;

    public JobPartialFailureException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
