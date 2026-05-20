package com.tchalanet.server.core.draw.internal.domain.exception;

import com.tchalanet.server.common.types.id.DrawId;

public class DrawLockedException extends RuntimeException {
    public DrawLockedException(DrawId drawId) {
        super("Draw is locked: " + drawId.value());
    }
}
