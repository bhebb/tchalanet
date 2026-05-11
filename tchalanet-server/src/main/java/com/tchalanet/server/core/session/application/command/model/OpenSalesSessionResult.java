package com.tchalanet.server.core.session.application.command.model;

import com.tchalanet.server.common.types.id.SalesSessionId;
import java.time.Instant;

public record OpenSalesSessionResult(SalesSessionId sessionId, Instant openedAt) {}
