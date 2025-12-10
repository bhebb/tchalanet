package com.tchalanet.server.core.pos.infra.web.dto;

import com.tchalanet.server.core.pos.domain.model.PosSessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PosSessionResponse(
    UUID id,
    UUID tenantId,
    UUID terminalId,
    UUID userId,
    PosSessionStatus status,
    Instant openedAt,
    Instant closedAt,
    Instant lastActivityAt,
    BigDecimal openingFloat,
    BigDecimal closingAmount,
    BigDecimal totalTicketsAmount,
    BigDecimal totalPayoutAmount,
    BigDecimal grossMargin) {}
