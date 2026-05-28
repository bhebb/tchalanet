package com.tchalanet.server.core.sales.internal.application.command.handler.payout;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.sales.api.command.payout.MarkTicketPayoutReversedCommand;
import com.tchalanet.server.core.sales.api.command.payout.MarkTicketPayoutReversedResult;
import com.tchalanet.server.core.sales.api.event.TicketPayoutReversedRecordedEvent;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.infra.cache.SalesTicketCacheEvictor;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class MarkTicketPayoutReversedCommandHandler
    implements CommandHandler<MarkTicketPayoutReversedCommand, MarkTicketPayoutReversedResult> {

    private final TicketReaderPort ticketReaderPort;
    private final TicketWriterPort ticketWriter;
    private final DomainEventPublisher eventPublisher;
    private final SalesTicketCacheEvictor cacheEvictor;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    public MarkTicketPayoutReversedResult handle(MarkTicketPayoutReversedCommand command) {
        var ticket = ticketReaderPort.getRequired(command.ticketId());

        var updated = ticket.markPayoutReversed(command.reversedBy(), command.reversedAt());
        var saved   = ticketWriter.save(updated);

        var event = new TicketPayoutReversedRecordedEvent(
            EventId.of(idGenerator.newUuid()),
            command.reversedAt(),
            saved.identity().tenantId(),
            saved.identity().id(),
            command.payoutId(),
            command.reversedBy(),
            saved.winningAmount() != null ? saved.winningAmount().amount() : java.math.BigDecimal.ZERO
        );

        AfterCommit.run(() -> {
            eventPublisher.publish(event);
            cacheEvictor.evictByTicket(saved.identity().id());
            cacheEvictor.evictByDraw(saved.context().drawId());
        });

        return new MarkTicketPayoutReversedResult(
            saved.identity().id(),
            saved.lifecycle().settlement().status()
        );
    }
}
