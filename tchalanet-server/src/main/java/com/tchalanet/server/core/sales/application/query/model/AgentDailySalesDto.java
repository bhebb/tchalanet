package com.tchalanet.server.core.sales.application.query.model;

import java.math.BigDecimal;
import java.util.UUID;

/** DTO for agent daily sales summary. */
public record AgentDailySalesDto(
    UUID agentId,
    BigDecimal totalAmount,
    long ticketCount
) {}
