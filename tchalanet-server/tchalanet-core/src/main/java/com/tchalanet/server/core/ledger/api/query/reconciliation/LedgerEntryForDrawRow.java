package com.tchalanet.server.core.ledger.api.query.reconciliation;

import com.tchalanet.server.common.types.id.LedgerEntryId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerDirection;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerOperationType;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerRefType;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryForDrawRow(
    LedgerEntryId ledgerEntryId,
    LedgerRefType refType,
    UUID refId,
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    String displayCode,
    LedgerOperationType operationType,
    long amountCents,
    String currency,
    LedgerDirection direction,
    Instant occurredAt
) {}
