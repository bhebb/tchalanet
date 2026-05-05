package com.tchalanet.server.core.draw.application.exception;

import com.tchalanet.server.common.types.id.DrawId;

public class DrawNotFoundException extends RuntimeException {
    public DrawNotFoundException(DrawId drawId) {
        super("Draw not found: " + drawId);
    }

    public DrawNotFoundException(String message) {
        super(message);
    }
}
