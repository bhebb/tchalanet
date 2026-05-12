package com.tchalanet.server.core.session.api.command;

import java.time.Instant;

public record CloseOutletOpenSalesSessionsResult(
    int targetCount,
    int closedCount,
    Instant closedAt
) {}
