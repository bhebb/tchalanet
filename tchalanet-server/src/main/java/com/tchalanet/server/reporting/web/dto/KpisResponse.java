package com.tchalanet.server.reporting.web.dto;

import java.math.BigDecimal;
import java.util.Map;

public record KpisResponse(
    BigDecimal totalSales,
    Integer totalTickets,
    Integer activeSessions,
    Map<String, Object> additionalMetrics) {}
