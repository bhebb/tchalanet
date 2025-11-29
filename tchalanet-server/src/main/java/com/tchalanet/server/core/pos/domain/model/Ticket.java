package com.tchalanet.server.core.pos.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Ticket(
    UUID id,
    UUID tenantId,
    String publicCode,
    UUID terminalId,
    UUID drawId,
    String status,
    BigDecimal totalAmount,
    Instant createdAt) {}
