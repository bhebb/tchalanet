package com.tchalanet.server.core.session.domain.model;

import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;
import java.time.Instant;

public record PosSessionTotals(
    SessionId sessionId,
    TenantId tenantId,
    long totalTickets,
    BigDecimal totalStake,
    BigDecimal totalPayout,
    BigDecimal grossMargin,
    Instant updatedAt) {}
