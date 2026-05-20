package com.tchalanet.server.core.draw.internal.domain.exception;

import com.tchalanet.server.common.types.id.DrawId;

public class DrawInvalidResultException extends RuntimeException {

    public DrawInvalidResultException(DrawId drawId, String message) {
        super(message + " drawId=" + drawId.value());
    }
}

