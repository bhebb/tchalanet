package com.tchalanet.server.features.reporting.web.dto;

import java.math.BigDecimal;
import java.util.Map;

public record KpisResponse(
    BigDecimal totalSales,
    Integer totalTickets,
    Integer activeSessions,
    Map<String, Object> additionalMetrics) {}
