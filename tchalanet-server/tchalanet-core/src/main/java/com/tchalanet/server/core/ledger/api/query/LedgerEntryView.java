package com.tchalanet.server.core.ledger.api.query;

import com.tchalanet.server.common.types.id.LedgerEntryId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerDirection;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerOperationType;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerRefType;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryView(
    LedgerEntryId id,
    TenantId tenantId,
    LedgerRefType refType,
    UUID refId,
    LedgerOperationType operationType,
    long amountCents,
    String currency,
    LedgerDirection direction,
    Instant occurredAt,
    LedgerEntryId reversalOfEntryId,
    String reason
) {}
