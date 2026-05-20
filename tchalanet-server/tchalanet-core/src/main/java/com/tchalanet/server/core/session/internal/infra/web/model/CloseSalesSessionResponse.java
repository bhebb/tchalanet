package com.tchalanet.server.core.session.internal.infra.web.model;

import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.core.session.api.command.CloseSalesSessionResult;

import java.time.Instant;

public record CloseSalesSessionResponse(SalesSessionId sessionId, Instant closedAt) {

    public static CloseSalesSessionResponse fromDomain(CloseSalesSessionResult session) {
        return new CloseSalesSessionResponse(
            session.sessionId(),
            session.closedAt());
    }
}
