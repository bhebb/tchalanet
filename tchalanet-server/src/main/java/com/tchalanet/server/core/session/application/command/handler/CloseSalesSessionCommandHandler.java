package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.payout.application.query.model.GetPayoutSummaryBySessionQuery;
import com.tchalanet.server.core.sales.application.query.model.GetTicketSalesSummaryBySessionQuery;
import com.tchalanet.server.core.session.application.command.model.CloseSalesSessionCommand;
import com.tchalanet.server.core.session.application.command.model.CloseSalesSessionResult;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import com.tchalanet.server.core.session.application.port.out.SalesSessionWriterPort;
import com.tchalanet.server.core.session.domain.event.SalesSessionClosedEvent;
import com.tchalanet.server.core.session.domain.service.SessionCashCalculator;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class CloseSalesSessionCommandHandler implements CommandHandler<CloseSalesSessionCommand, CloseSalesSessionResult> {

    private final SalesSessionReaderPort reader;
    private final SalesSessionWriterPort writer;
    private final DomainEventPublisher events;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final QueryBus queryBus;

    @Override
    @TchTx
    public CloseSalesSessionResult handle(CloseSalesSessionCommand command) {
        var session = reader.getById(command.tenantId(), command.sessionId());

        var now = Instant.now(clock);
        var sales =
            queryBus.ask(new GetTicketSalesSummaryBySessionQuery(
                command.tenantId(),
                command.sessionId()));

        var payouts =
            queryBus.ask(new GetPayoutSummaryBySessionQuery(
                command.tenantId(),
                command.sessionId()));

        var cashSummary =
            SessionCashCalculator.calculate(
                session.openingFloatCents(),
                sales.soldAmountCents(),
                payouts.paidAmountCents(),
                command.declaredClosingAmountCents());

        var closed =
            session.close(
                command.closedBy(),
                now,
                cashSummary,
                command.reason());
        var saved = writer.save(closed);

        var event = new SalesSessionClosedEvent(
            EventId.of(idGenerator.newUuid()),
            now,
            command.tenantId(),
            saved.id(),
            saved.outletId(),
            saved.terminalId(),
            command.closedBy(),
            command.reason());
        AfterCommit.run(() -> events.publish(event));

        return new CloseSalesSessionResult(saved.id(), now);
    }
}
