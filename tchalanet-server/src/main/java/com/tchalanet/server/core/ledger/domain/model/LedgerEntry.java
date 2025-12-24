package com.tchalanet.server.core.ledger.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntry(
    UUID id,
    UUID tenantId,
    LedgerRefType refType,
    UUID refId,
    BigDecimal amount,
    LedgerDirection direction,
    Instant occurredAt
) {
    public static LedgerEntry create(
        UUID tenantId,
        LedgerRefType refType,
        UUID refId,
        BigDecimal amount,
        LedgerDirection direction,
        Instant occurredAt
    ) {
        return new LedgerEntry(UUID.randomUUID(), tenantId, refType, refId, amount, direction, occurredAt);
    }

}
