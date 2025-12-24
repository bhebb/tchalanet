package com.tchalanet.server.core.ledger.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface RecordLedgerFromSalesPort {
    void recordTicketSale(UUID tenantId, UUID ticketId, long stakeCents, Instant occurredAt);
}
