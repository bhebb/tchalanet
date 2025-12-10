package com.tchalanet.server.core.sales.application.query.model;

import java.util.UUID;
import java.time.LocalDate;

/** Query to get an agent's daily sales summary */
public record GetAgentDailySalesQuery(
    UUID tenantId,
    UUID agentId,
    LocalDate date
) {}

