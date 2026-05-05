package com.tchalanet.server.common.error;

public class TchGenericAppException extends RuntimeException {

    public TchGenericAppException(String message) {
        super(message);
    }

    public TchGenericAppException(String message, Throwable cause) {
        super(message, cause);
    }
}
