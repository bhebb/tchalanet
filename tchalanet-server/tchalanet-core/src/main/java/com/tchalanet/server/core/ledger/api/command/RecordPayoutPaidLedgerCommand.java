package com.tchalanet.server.core.ledger.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.Objects;

public record RecordPayoutPaidLedgerCommand(
    TenantId tenantId,
    PayoutId payoutId,
    long amountCents,
    String currency,
    Instant occurredAt
) implements Command<Void> {

    public RecordPayoutPaidLedgerCommand {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(payoutId, "payoutId is required");
        Objects.requireNonNull(currency, "currency is required");

        if (amountCents <= 0) {
            throw new IllegalArgumentException("amountCents must be positive");
        }

        if (currency.isBlank() || currency.length() != 3) {
            throw new IllegalArgumentException("currency must be ISO-4217 style");
        }
    }
}
