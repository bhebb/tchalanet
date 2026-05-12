package com.tchalanet.server.core.ledger.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import java.time.Instant;
import java.util.Objects;

public record RecordTicketSaleLedgerCommand(
    TenantId tenantId,
    TicketId ticketId,
    long stakeCents,
    String currency,
    Instant occurredAt
) implements Command<Void> {

    public RecordTicketSaleLedgerCommand {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(ticketId, "ticketId is required");
        Objects.requireNonNull(currency, "currency is required");

        if (stakeCents <= 0) {
            throw new IllegalArgumentException("stakeCents must be positive");
        }

        if (currency.isBlank() || currency.length() != 3) {
            throw new IllegalArgumentException("currency must be ISO-4217 style");
        }
    }
}
