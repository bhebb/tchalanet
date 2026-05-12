package com.tchalanet.server.core.draw.internal.domain.exception;

import com.tchalanet.server.common.types.id.DrawId;

public class DrawInvalidCancelException extends RuntimeException {
    public DrawInvalidCancelException(DrawId drawId, String message) {
        super(message + " drawId=" + drawId.value());
    }
}
