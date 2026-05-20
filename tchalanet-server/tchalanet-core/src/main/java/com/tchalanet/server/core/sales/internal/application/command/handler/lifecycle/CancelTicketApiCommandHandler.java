package com.tchalanet.server.core.sales.internal.application.command.handler.lifecycle;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.sales.api.command.cancel.CancelTicketCommand;
import com.tchalanet.server.core.sales.api.command.cancel.CancelTicketResult;
import com.tchalanet.server.core.sales.api.event.TicketCancelledEvent;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueSeverity;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueView;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CancelTicketApiCommandHandler
    implements CommandHandler<CancelTicketCommand, CancelTicketResult> {

    private final TicketReaderPort ticketReader;
    private final TicketWriterPort ticketWriter;
    private final DomainEventPublisher publisher;
    private final Clock clock;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    public CancelTicketResult handle(CancelTicketCommand cmd) {
        var ctx = TchContext.currentOrThrow();
        var ticket = ticketReader.getRequired(cmd.ticketId());
        var now = Instant.now(clock);
        var cancelledBy = ctx.currentUserIdRequired();

        try {
            var updated = ticket.cancel(cancelledBy, cmd.reason(), now);
            var saved = ticketWriter.save(updated);

            AfterCommit.run(() -> publisher.publish(new TicketCancelledEvent(
                EventId.of(idGenerator.newUuid()),
                now,
                saved.identity().tenantId(),
                saved.identity().id(),
                cancelledBy,
                cmd.reason()
            )));

            return new CancelTicketResult(
                saved.identity().id(),
                CancelTicketResult.CancelTicketOutcome.CANCELLED,
                now,
                List.of()
            );
        } catch (IllegalStateException ex) {
            return new CancelTicketResult(
                cmd.ticketId(),
                CancelTicketResult.CancelTicketOutcome.REJECTED,
                null,
                List.of(SaleIssueView.basket(
                    "CANCEL_REJECTED",
                    SaleIssueSeverity.ERROR,
                    ex.getMessage(),
                    "Le ticket ne peut pas etre annule dans son etat actuel.",
                    Map.of("ticketId", cmd.ticketId().value().toString())
                ))
            );
        }
    }
}
