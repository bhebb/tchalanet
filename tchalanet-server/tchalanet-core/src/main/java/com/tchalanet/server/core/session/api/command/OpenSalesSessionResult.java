package com.tchalanet.server.core.session.api.command;

import com.tchalanet.server.common.types.id.SalesSessionId;
import java.time.Instant;

public record OpenSalesSessionResult(SalesSessionId sessionId, Instant openedAt) {}
