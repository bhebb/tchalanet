package com.tchalanet.server.core.ledger.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.LedgerEntryId;
import com.tchalanet.server.core.ledger.application.command.model.RecordTicketSaleLedgerCommand;
import com.tchalanet.server.core.ledger.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecordTicketSaleLedgerCommandHandler
    implements VoidCommandHandler<RecordTicketSaleLedgerCommand> {

    private final LedgerWriterPort ledgerWriter;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(RecordTicketSaleLedgerCommand command) {
        var occurredAt = command.occurredAt() != null
            ? command.occurredAt()
            : Instant.now(clock);

        var entry =
            LedgerEntry.ticketSale(
                LedgerEntryId.of(idGenerator.newUuid()),
                command.tenantId(),
                command.ticketId(),
                command.stakeCents(),
                command.currency(),
                occurredAt);

        var appended = ledgerWriter.appendIfAbsent(entry);

        if (!appended) {
            log.info(
                "Ticket sale ledger already recorded: tenantId={} ticketId={}",
                command.tenantId(),
                command.ticketId());
        }
    }
}
