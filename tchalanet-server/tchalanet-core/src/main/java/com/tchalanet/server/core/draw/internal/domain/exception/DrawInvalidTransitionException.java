package com.tchalanet.server.core.draw.internal.domain.exception;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;

public class DrawInvalidTransitionException extends RuntimeException {

    public DrawInvalidTransitionException(
        DrawId drawId,
        DrawStatus current,
        DrawStatus expected,
        String message) {
        super(message + " drawId=" + drawId.value() + " current=" + current + " expected=" + expected);
    }
}
