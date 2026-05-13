package com.tchalanet.server.core.session.api.query;

import com.tchalanet.server.common.types.id.SalesSessionId;
import java.time.Instant;

public record OpenedSalesSessionView(
    SalesSessionId sessionId,
    Instant openedAt
) {}
