package com.tchalanet.server.core.session.application.command.model;

import java.time.Instant;

public record CloseDueSalesSessionsResult(
    int targetsFound,
    int sessionsClosed,
    Instant closedAt
) {}
