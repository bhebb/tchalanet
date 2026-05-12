package com.tchalanet.server.core.ledger.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.LedgerEntryId;
import com.tchalanet.server.core.ledger.api.command.RecordPayoutPaidLedgerCommand;
import com.tchalanet.server.core.ledger.internal.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecordPayoutPaidLedgerCommandHandler
    implements VoidCommandHandler<RecordPayoutPaidLedgerCommand> {

    private final LedgerWriterPort ledgerWriter;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(RecordPayoutPaidLedgerCommand command) {
        var occurredAt = command.occurredAt() != null
            ? command.occurredAt()
            : Instant.now(clock);

        var entry =
            LedgerEntry.payoutPaid(
                LedgerEntryId.of(idGenerator.newUuid()),
                command.tenantId(),
                command.payoutId(),
                command.amountCents(),
                command.currency(),
                occurredAt);

        var appended = ledgerWriter.appendIfAbsent(entry);

        if (!appended) {
            log.info(
                "Payout ledger already recorded: tenantId={} payoutId={}",
                command.tenantId(),
                command.payoutId());
        }
    }
}
