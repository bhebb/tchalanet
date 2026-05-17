package com.tchalanet.server.core.ledger.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.Money;

import java.time.Instant;
import java.util.Objects;

public record RecordTicketSaleLedgerCommand(
    TenantId tenantId,
    TicketId ticketId,
    Money stake,
    Instant occurredAt
) implements Command<Void> {

    public RecordTicketSaleLedgerCommand {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(ticketId, "ticketId is required");

        if (stake.amount().longValue() <= 0) {
            throw new IllegalArgumentException("stake must be positive");
        }

        if (stake.currency() == null || stake.currency().code().length() != 3) {
            throw new IllegalArgumentException("currency must be ISO-4217 style");
        }
    }
}
