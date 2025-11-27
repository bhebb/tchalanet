package com.tchalanet.server.stats.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TenantDailyStatsResponse(
    UUID tenantId,
    LocalDate day,
    long totalTickets,
    BigDecimal totalStake,
    BigDecimal totalPayout,
    BigDecimal grossMargin) {}
