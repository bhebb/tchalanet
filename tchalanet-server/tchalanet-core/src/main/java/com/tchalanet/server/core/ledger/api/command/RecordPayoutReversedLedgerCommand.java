package com.tchalanet.server.core.ledger.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.Objects;

public record RecordPayoutReversedLedgerCommand(
    TenantId tenantId,
    PayoutId payoutId,
    long amountCents,
    String currency,
    String reason,
    Instant occurredAt
) implements Command<Void> {

    public RecordPayoutReversedLedgerCommand {
        Objects.requireNonNull(tenantId,  "tenantId is required");
        Objects.requireNonNull(payoutId,  "payoutId is required");
        Objects.requireNonNull(currency,  "currency is required");

        if (amountCents <= 0) {
            throw new IllegalArgumentException("amountCents must be positive");
        }
        if (currency.isBlank() || currency.length() != 3) {
            throw new IllegalArgumentException("currency must be ISO-4217 style");
        }
    }
}
