package com.tchalanet.server.core.ledger.domain.model;
import com.tchalanet.server.common.types.id.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntry(
    UUID id,
    TenantId tenantId,
    LedgerRefType refType,
    UUID refId,
    BigDecimal amount,
    LedgerDirection direction,
    Instant occurredAt
) {
    public static LedgerEntry create(
        TenantId tenantId,
        LedgerRefType refType,
        UUID refId,
        BigDecimal amount,
        LedgerDirection direction,
        Instant occurredAt
    ) {
        return new LedgerEntry(UUID.randomUUID(), tenantId, refType, refId, amount, direction, occurredAt);
    }

}
