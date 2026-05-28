package com.tchalanet.server.core.sales.internal.application.command.handler.payout;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.sales.api.command.payout.MarkTicketPayoutPaidCommand;
import com.tchalanet.server.core.sales.api.command.payout.MarkTicketPayoutPaidResult;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidRecordedEvent;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.infra.cache.SalesTicketCacheEvictor;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class MarkTicketPayoutPaidCommandHandler
    implements CommandHandler<MarkTicketPayoutPaidCommand, MarkTicketPayoutPaidResult> {

    private final TicketReaderPort ticketReaderPort;
    private final TicketWriterPort ticketWriter;
    private final DomainEventPublisher eventPublisher;
    private final SalesTicketCacheEvictor cacheEvictor;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    public MarkTicketPayoutPaidResult handle(MarkTicketPayoutPaidCommand command) {
        var ticket = ticketReaderPort.getRequired(command.ticketId());

        var updated = ticket.markPaid(
            command.payoutId(),
            command.paidBy(),
            command.paidAt()
        );

        var saved = ticketWriter.save(updated);

        var event = new TicketPayoutPaidRecordedEvent(
            EventId.of(idGenerator.newUuid()),
            command.paidAt(),
            saved.identity().tenantId(),
            saved.identity().id(),
            command.payoutId(),
            command.paidBy(),
            saved.lifecycle().settlement().status(),
            saved.winningAmount() != null ? saved.winningAmount().amount() : java.math.BigDecimal.ZERO
        );

        AfterCommit.run(() -> {
            eventPublisher.publish(event);
            cacheEvictor.evictByTicket(saved.identity().id());
            cacheEvictor.evictByDraw(saved.context().drawId());
        });

        return new MarkTicketPayoutPaidResult(
            saved.identity().id(),
            saved.lifecycle().settlement().status()
        );
    }
}
