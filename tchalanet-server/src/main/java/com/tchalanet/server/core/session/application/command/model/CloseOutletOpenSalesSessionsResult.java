package com.tchalanet.server.core.session.application.command.model;

import java.time.Instant;

public record CloseOutletOpenSalesSessionsResult(
    int targetCount,
    int closedCount,
    Instant closedAt
) {}
