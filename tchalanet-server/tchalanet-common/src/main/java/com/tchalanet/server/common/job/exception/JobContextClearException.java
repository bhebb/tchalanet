package com.tchalanet.server.common.job.exception;

public class JobContextClearException extends RuntimeException {

    public JobContextClearException(String message) {
        super(message);
    }

    public JobContextClearException(String message, Throwable cause) {
        super(message, cause);
    }
}
