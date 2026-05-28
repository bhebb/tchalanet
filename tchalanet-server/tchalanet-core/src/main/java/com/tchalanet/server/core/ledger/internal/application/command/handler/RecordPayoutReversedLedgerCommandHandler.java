package com.tchalanet.server.core.ledger.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.LedgerEntryId;
import com.tchalanet.server.core.ledger.api.command.RecordPayoutReversedLedgerCommand;
import com.tchalanet.server.core.ledger.internal.application.port.out.LedgerReaderPort;
import com.tchalanet.server.core.ledger.internal.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerOperationType;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecordPayoutReversedLedgerCommandHandler
    implements VoidCommandHandler<RecordPayoutReversedLedgerCommand> {

    private final LedgerReaderPort ledgerReader;
    private final LedgerWriterPort ledgerWriter;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(RecordPayoutReversedLedgerCommand command) {
        var ref = LedgerReference.payout(command.payoutId());

        var original = ledgerReader.findByReferenceAndOperation(ref, LedgerOperationType.PAYOUT_PAID);
        if (original.isEmpty()) {
            log.warn("No PAYOUT_PAID ledger entry found for payoutId={} — skipping reversal",
                command.payoutId());
            return;
        }

        var occurredAt = command.occurredAt() != null ? command.occurredAt() : Instant.now(clock);
        var reversal   = original.get().reversal(
            LedgerEntryId.of(idGenerator.newUuid()),
            occurredAt,
            command.reason());

        var appended = ledgerWriter.appendIfAbsent(reversal);

        if (!appended) {
            log.info("Payout reversal ledger already recorded: tenantId={} payoutId={}",
                command.tenantId(), command.payoutId());
        }
    }
}
