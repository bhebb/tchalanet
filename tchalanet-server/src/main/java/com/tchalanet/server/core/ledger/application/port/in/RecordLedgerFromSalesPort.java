package com.tchalanet.server.core.ledger.application.port.in;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public interface RecordLedgerFromSalesPort {
    void recordTicketSale(TenantId tenantId, TicketId ticketId, long stakeCents, Instant occurredAt);
}
