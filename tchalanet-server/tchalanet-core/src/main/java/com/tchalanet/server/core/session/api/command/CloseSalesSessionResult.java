package com.tchalanet.server.core.session.api.command;

import com.tchalanet.server.common.types.id.SalesSessionId;
import java.time.Instant;

public record CloseSalesSessionResult(SalesSessionId sessionId, Instant closedAt) {}
