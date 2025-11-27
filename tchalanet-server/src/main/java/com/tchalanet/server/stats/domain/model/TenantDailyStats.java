package com.tchalanet.server.stats.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents the aggregated daily statistics for a tenant. This is primarily used for reporting and
 * dashboards.
 */
public record TenantDailyStats(
    UUID tenantId,
    LocalDate day,
    long totalTickets,
    BigDecimal totalStake,
    BigDecimal totalPayout,
    BigDecimal grossMargin) {}
