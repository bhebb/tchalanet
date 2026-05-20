package com.tchalanet.server.core.session.internal.infra.web.model;

import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.core.session.api.command.CloseSalesSessionResult;
import com.tchalanet.server.core.session.api.command.OpenSalesSessionResult;

import java.time.Instant;

public record OpenSalesSessionResponse(SalesSessionId sessionId, Instant openedAt) {

    public static OpenSalesSessionResponse fromDomain(OpenSalesSessionResult openSalesSessionResult) {
        return new OpenSalesSessionResponse(
            openSalesSessionResult.sessionId(),
            openSalesSessionResult.openedAt());
    }
}
