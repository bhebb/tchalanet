package com.tchalanet.server.core.sales.internal.application.query.handler.reconciliation;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.draw.api.query.ListDrawsForDrawResultQuery;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.api.query.reconciliation.ExpectedTicketOutcomeRow;
import com.tchalanet.server.core.sales.api.query.reconciliation.ListExpectedTicketOutcomesForDrawResultQuery;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketPublicCodeFormatter;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListExpectedTicketOutcomesForDrawResultQueryHandler
    implements QueryHandler<ListExpectedTicketOutcomesForDrawResultQuery, List<ExpectedTicketOutcomeRow>> {

    private final QueryBus queryBus;
    private final TicketJpaRepository repository;
    private final TicketPublicCodeFormatter publicCodeFormatter;

    @Override
    public List<ExpectedTicketOutcomeRow> handle(ListExpectedTicketOutcomesForDrawResultQuery query) {
        var drawIds = queryBus.ask(new ListDrawsForDrawResultQuery(query.drawResultId())).stream()
            .map(row -> row.drawId())
            .toList();

        return drawIds.stream()
            .flatMap(drawId -> repository.findWithLinesByDrawId(drawId.value()).stream())
            .map(ticket -> {
                var winningLineCount = (int) ticket.getLines().stream()
                    .filter(line -> line.getResultStatus() == TicketLineResultStatus.WON)
                    .count();
                var shouldWin = winningLineCount > 0;
                return new ExpectedTicketOutcomeRow(
                    TicketId.of(ticket.getId()),
                    ticket.getTicketCode(),
                    ticket.getPublicCode(),
                    publicCodeFormatter.display(ticket.getPublicCode()),
                    DrawId.of(ticket.getDrawId()),
                    query.drawResultId(),
                    shouldWin,
                    shouldWin ? TicketResultStatus.WON : TicketResultStatus.LOST,
                    shouldWin ? TicketSettlementStatus.PAYOUT_PENDING : TicketSettlementStatus.NO_PAYOUT,
                    ticket.getLines().stream()
                        .filter(line -> line.getResultStatus() == TicketLineResultStatus.WON)
                        .map(line -> line.getPayoutAmount() == null ? BigDecimal.ZERO : line.getPayoutAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                    winningLineCount
                );
            })
            .toList();
    }
}
