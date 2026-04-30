package com.tchalanet.server.core.payout.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.core.ledger.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.domain.model.LedgerDirection;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import com.tchalanet.server.core.ledger.domain.model.LedgerRefType;
import com.tchalanet.server.core.payout.application.command.model.ExecutePayoutCommand;
import com.tchalanet.server.core.payout.application.port.out.PayoutReaderPort;
import com.tchalanet.server.core.payout.application.port.out.PayoutWriterPort;
import com.tchalanet.server.core.payout.domain.model.Payout;
import com.tchalanet.server.core.payout.domain.model.PayoutStatus;
import com.tchalanet.server.core.payout.infra.event.PayoutRegisteredEvent;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ExecutePayoutCommandHandler implements CommandHandler<ExecutePayoutCommand, Payout> {

    private final PayoutReaderPort payoutReaderPort;
    private final PayoutWriterPort payoutWriterPort;

    private final TicketReaderPort ticketReaderPort;
    private final TicketWriterPort ticketWritterPort;
    private final LedgerWriterPort ledgerWriter;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    @Override
    @TchTx
    public Payout handle(ExecutePayoutCommand command) {
        Optional<Payout> optPayout = payoutReaderPort.findById(command.payoutId());
        if (optPayout.isEmpty()) {
            throw new IllegalStateException("Payout not found: " + command.payoutId());
        }
        Payout payout = optPayout.get();

        if (payout.getStatus() == PayoutStatus.PAID) {
            // idempotent: already paid
            return payout;
        }

        if (payout.getStatus() != PayoutStatus.APPROVED
            && payout.getStatus() != PayoutStatus.REQUESTED) {
            throw new IllegalStateException("Payout is not approved for execution: " + payout.getId());
        }

        // load ticket and validate
        Optional<Ticket> optTicket = ticketReaderPort.findWithLinesById(payout.getTicketId());
        if (optTicket.isEmpty()) {
            throw new IllegalStateException("Ticket not found for payout: " + payout.getTicketId());
        }
        Ticket ticket = optTicket.get();
        if (ticket.getResultStatus() != TicketResultStatus.WON) {
            throw new IllegalStateException("Ticket is not in WON/RESULTED_WIN state: " + ticket.getId());
        }

        // execute payout
        var now = Instant.now(clock);
        // allow marking paid from REQUESTED when executing
        payout.markPaid(now, true);
        var saved = payoutWriterPort.save(payout);

        // mark ticket as settled/paid and persist
        ticket.settle(now);
        ticketWritterPort.save(ticket);

        // amount as BigDecimal reconstructed from cents
        BigDecimal amount = BigDecimal.valueOf(saved.getAmountCents(), 2);

        // ledger entry — use domain enums and TenantId wrapper
        LedgerEntry entry =
            LedgerEntry.create(
                saved.getTenantId(),
                LedgerRefType.PAYOUT,
                saved.getId().value(),
                amount,
                LedgerDirection.DEBIT,
                now);
        ledgerWriter.append(entry);

        // publish event
        PayoutRegisteredEvent event =
            new PayoutRegisteredEvent(
                EventId.of(UUID.randomUUID()),
                Instant.now(clock),
                saved.getTenantId(),
                saved.getId(),
                saved.getTicketId(),
                null,
                amount);
        AfterCommit.run(() -> domainEventPublisher.publish(event));

        log.info("Executed payout {} for ticket {}", saved.getId(), saved.getTicketId());
        return saved;
    }
}
