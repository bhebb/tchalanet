package com.tchalanet.server.core.session.application.exception;

import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public class SalesSessionAlreadyOpenException extends RuntimeException {
    public SalesSessionAlreadyOpenException(@NotNull UserId userId, SalesSessionId sessionId) {
        super("User " + userId + " already has an open sales session with id " + sessionId);
    }
}
