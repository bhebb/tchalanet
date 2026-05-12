package com.tchalanet.server.core.sales.api.query;

import java.math.BigDecimal;
import java.util.UUID;

/** DTO for agent daily sales summary. */
public record AgentDailySalesDto(UUID agentId, BigDecimal totalAmount, Long ticketCount) {}
