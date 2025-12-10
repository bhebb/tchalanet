package com.tchalanet.server.core.sales.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketVerificationResult(
    UUID ticketId,
    UUID tenantId,
    String publicCode,
    TicketStatus status,
    String gameCode,
    String drawCode,
    Instant drawDateTime,
    List<String> linesNumbers, // ex: ["05-12-24", "03-18-29"]
    BigDecimal stakeAmount,
    BigDecimal potentialPayout,
    String outletNameMasked,
    Instant createdAt) {
}
