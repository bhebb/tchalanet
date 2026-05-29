package com.tchalanet.server.core.sales.internal.application.query.handler.reconciliation;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.query.reconciliation.GetSalesOutcomeSummaryForDrawQuery;
import com.tchalanet.server.core.sales.api.query.reconciliation.SalesOutcomeSummaryForDrawRow;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetSalesOutcomeSummaryForDrawQueryHandler
    implements QueryHandler<GetSalesOutcomeSummaryForDrawQuery, SalesOutcomeSummaryForDrawRow> {

    private final TicketJpaRepository repository;

    @Override
    public SalesOutcomeSummaryForDrawRow handle(GetSalesOutcomeSummaryForDrawQuery query) {
        var tickets = repository.findWithLinesByDrawId(query.drawId().value());
        var currency = tickets.isEmpty() ? CurrencyCode.of("HTG") : CurrencyCode.of(tickets.getFirst().getCurrency());
        return new SalesOutcomeSummaryForDrawRow(
            query.drawId(),
            tickets.size(),
            tickets.stream().filter(t -> t.getSaleStatus() == TicketSaleStatus.APPROVED).count(),
            tickets.stream().filter(t -> t.getSaleStatus() == TicketSaleStatus.CANCELLED).count(),
            tickets.stream().filter(t -> t.getSaleStatus() == TicketSaleStatus.VOIDED).count(),
            tickets.stream().filter(t -> t.getResultStatus() == TicketResultStatus.WON).count(),
            tickets.stream().filter(t -> t.getResultStatus() == TicketResultStatus.LOST).count(),
            money(sum(tickets, TicketJpaEntity::getStakeAmount), currency),
            money(sum(tickets, TicketJpaEntity::getPotentialPayoutAmount), currency),
            money(sum(tickets, TicketJpaEntity::getWinningAmount), currency)
        );
    }

    private static BigDecimal sum(
        java.util.List<TicketJpaEntity> tickets,
        java.util.function.Function<TicketJpaEntity, BigDecimal> getter
    ) {
        return tickets.stream()
            .map(getter)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Money money(BigDecimal amount, CurrencyCode currency) {
        return new Money(amount, currency);
    }
}
