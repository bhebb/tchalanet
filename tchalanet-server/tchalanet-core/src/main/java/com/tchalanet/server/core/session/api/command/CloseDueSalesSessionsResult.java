package com.tchalanet.server.core.session.api.command;

import java.time.Instant;

public record CloseDueSalesSessionsResult(
    int targetsFound,
    int sessionsClosed,
    Instant closedAt
) {}
