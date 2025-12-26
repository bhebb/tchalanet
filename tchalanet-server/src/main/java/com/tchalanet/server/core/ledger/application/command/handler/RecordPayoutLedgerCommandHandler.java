package com.tchalanet.server.core.ledger.application.command.handler;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.port.in.RecordLedgerFromPayoutPort;
import com.tchalanet.server.core.ledger.application.port.out.LedgerReaderPort;
import com.tchalanet.server.core.ledger.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.domain.model.LedgerDirection;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import com.tchalanet.server.core.ledger.domain.model.LedgerRefType;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecordPayoutLedgerCommandHandler implements RecordLedgerFromPayoutPort {

    private final LedgerWriterPort ledgerWriter;
    private final LedgerReaderPort ledgerReader;
    private final Clock clock;

    @Override
    @TchTx
    public void recordPayout(TenantId tenantId, PayoutId payoutId, BigDecimal amount, Instant occurredAt) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(payoutId, "payoutId");
        Objects.requireNonNull(amount, "amount");

        if (amount.signum() <= 0) {
            log.warn("Skipping payout ledger record: non_positive_amount tenantId={} payoutId={} amount={}",
                tenantId, payoutId, amount);
            return;
        }

        // Idempotency (soft)
        if (ledgerReader.existsByRef(tenantId, LedgerRefType.PAYOUT, payoutId.uuid())) {
            log.warn("Ledger entry already exists for payoutId={} tenantId={}, skipping", payoutId, tenantId);
            return;
        }

        var at = occurredAt != null ? occurredAt : Instant.now(clock);

        var entry =
            LedgerEntry.create(
                tenantId,
                LedgerRefType.PAYOUT,
                payoutId.uuid(),
                amount,
                LedgerDirection.DEBIT,
                at);

        ledgerWriter.append(entry);
    }
}
