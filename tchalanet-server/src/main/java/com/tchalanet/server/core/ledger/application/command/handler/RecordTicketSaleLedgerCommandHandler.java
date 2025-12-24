package com.tchalanet.server.core.ledger.application.command.handler;

import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.port.in.RecordLedgerFromSalesPort;
import com.tchalanet.server.core.ledger.application.port.out.LedgerReaderPort;
import com.tchalanet.server.core.ledger.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.domain.model.LedgerDirection;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import com.tchalanet.server.core.ledger.domain.model.LedgerRefType;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecordTicketSaleLedgerCommandHandler implements RecordLedgerFromSalesPort {

    private final LedgerWriterPort ledgerWriter;
    private final LedgerReaderPort ledgerReader;
    private final Clock clock;

    @Override
    @TchTx
    public void recordTicketSale(UUID tenantId, UUID ticketId, long stakeCents, Instant occurredAt) {
        // Idempotency (soft)
        if (ledgerReader.existsByRef(tenantId, LedgerRefType.TICKET_SALE, ticketId)) {
            log.warn("Ledger entry already exists for ticketId={} tenantId={}, skipping", ticketId, tenantId);
            return;
        }

        Instant at = occurredAt != null ? occurredAt : Instant.now(clock);
        BigDecimal amount = BigDecimal.valueOf(stakeCents).movePointLeft(2);

        LedgerEntry entry =
            LedgerEntry.create(
                tenantId,
                LedgerRefType.TICKET_SALE,
                ticketId,
                amount,
                LedgerDirection.CREDIT,
                at);

        ledgerWriter.append(entry);
    }
}
