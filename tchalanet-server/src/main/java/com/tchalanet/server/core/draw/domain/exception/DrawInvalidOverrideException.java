package com.tchalanet.server.core.draw.domain.exception;

import com.tchalanet.server.common.types.id.DrawId;

public class DrawInvalidOverrideException extends RuntimeException {

    public DrawInvalidOverrideException(DrawId drawId, String message) {
        super(message + " drawId=" + drawId.value());
    }
}
