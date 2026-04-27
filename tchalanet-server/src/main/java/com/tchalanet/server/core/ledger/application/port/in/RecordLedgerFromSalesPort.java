package com.tchalanet.server.core.ledger.application.port.in;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import java.time.Instant;

public interface RecordLedgerFromSalesPort {
  void recordTicketSale(TenantId tenantId, TicketId ticketId, long stakeCents, Instant occurredAt);
}
