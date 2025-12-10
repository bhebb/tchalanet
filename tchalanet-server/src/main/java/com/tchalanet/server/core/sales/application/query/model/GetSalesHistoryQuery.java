package com.tchalanet.server.core.sales.application.query.model;

import java.util.UUID;
import java.time.Instant;

/** Query to fetch sales history with optional filters */
public record GetSalesHistoryQuery(
    UUID tenantId,
    UUID terminalId,
    UUID agentId,
    Instant from,
    Instant to,
    int page,
    int size
) {}

