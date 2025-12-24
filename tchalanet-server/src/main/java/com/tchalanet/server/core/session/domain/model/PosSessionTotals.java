package com.tchalanet.server.core.session.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PosSessionTotals(
    UUID sessionId,
    UUID tenantId,
    long totalTickets,
    BigDecimal totalStake,
    BigDecimal totalPayout,
    BigDecimal grossMargin,
    Instant updatedAt
) {
}
