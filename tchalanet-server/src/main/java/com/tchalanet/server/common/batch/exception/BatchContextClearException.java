package com.tchalanet.server.common.batch.exception;

public class BatchContextClearException extends RuntimeException {

    public BatchContextClearException(String message) {
        super(message);
    }

    public BatchContextClearException(String message, Throwable cause) {
        super(message, cause);
    }
}
