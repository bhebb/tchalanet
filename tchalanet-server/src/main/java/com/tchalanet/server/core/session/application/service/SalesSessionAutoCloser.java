package com.tchalanet.server.core.session.application.service;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.payout.application.query.model.GetPayoutSummaryBySessionQuery;
import com.tchalanet.server.core.sales.application.query.model.GetTicketSalesSummaryBySessionQuery;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import com.tchalanet.server.core.session.application.port.out.SalesSessionWriterPort;
import com.tchalanet.server.core.session.domain.event.SalesSessionClosedEvent;
import com.tchalanet.server.core.session.domain.model.AutoSessionCloseTarget;
import com.tchalanet.server.core.session.domain.model.SalesSessionStatus;
import com.tchalanet.server.core.session.domain.service.SessionCashCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;


@Component
@RequiredArgsConstructor
public class SalesSessionAutoCloser {

    private final SalesSessionReaderPort sessionReader;
    private final SalesSessionWriterPort sessionWriter;
    private final DomainEventPublisher events;
    private final IdGenerator idGenerator;
    private final QueryBus queryBus;

    public int closeTargets(List<AutoSessionCloseTarget> targets, Instant fallbackClosedAt) {
        int closedCount = 0;

        for (var target : targets) {
            var session = sessionReader.getById(target.tenantId(), target.sessionId());

            if (session.status() != SalesSessionStatus.OPEN) {
                continue;
            }

            var closedAt = target.closedAt() == null ? fallbackClosedAt : target.closedAt();

            var sales =
                queryBus.ask(
                    new GetTicketSalesSummaryBySessionQuery(
                        target.tenantId(),
                        target.sessionId()));

            var payouts =
                queryBus.ask(
                    new GetPayoutSummaryBySessionQuery(
                        target.tenantId(),
                        target.sessionId()));

            var cashSummary =
                SessionCashCalculator.calculate(
                    session.openingFloatCents(),
                    sales.soldAmountCents(),
                    payouts.paidAmountCents(),
                    null);

            var closed =
                session.close(
                    target.closedBy(),
                    closedAt,
                    cashSummary,
                    target.reason());

            var saved = sessionWriter.save(closed);
            closedCount++;

            var event =
                new SalesSessionClosedEvent(
                    EventId.of(idGenerator.newUuid()),
                    closedAt,
                    target.tenantId(),
                    saved.id(),
                    saved.outletId(),
                    saved.terminalId(),
                    saved.closedBy(),
                    saved.closeReason());

            AfterCommit.run(() -> events.publish(event));
        }

        return closedCount;
    }
}
