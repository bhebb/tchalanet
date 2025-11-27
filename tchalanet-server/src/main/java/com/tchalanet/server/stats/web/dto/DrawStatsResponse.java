package com.tchalanet.server.stats.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DrawStatsResponse(
    UUID drawId,
    UUID tenantId,
    long totalTickets,
    long totalLines,
    BigDecimal totalStake,
    BigDecimal totalPayout,
    long winnersCount,
    long losersCount,
    BigDecimal grossMargin,
    BigDecimal marginPct) {}
