package com.tchalanet.server.core.ledger.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import java.time.Instant;

public record RecordTicketSaleLedgerCommand(
    TenantId tenantId, TicketId ticketId, long stakeCents, Instant occurredAt)
    implements Command<Void> {}
