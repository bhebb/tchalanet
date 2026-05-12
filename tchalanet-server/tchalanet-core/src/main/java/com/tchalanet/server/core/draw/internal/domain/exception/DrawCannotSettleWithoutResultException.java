package com.tchalanet.server.core.draw.internal.domain.exception;

import com.tchalanet.server.common.types.id.DrawId;

public class DrawCannotSettleWithoutResultException extends RuntimeException {

    public DrawCannotSettleWithoutResultException(DrawId drawId) {
        super("Cannot settle draw without result: " + drawId.value());
    }
}
