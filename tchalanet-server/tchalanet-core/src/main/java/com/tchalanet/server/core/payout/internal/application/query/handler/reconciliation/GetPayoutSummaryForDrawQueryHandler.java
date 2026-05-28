package com.tchalanet.server.core.payout.internal.application.query.handler.reconciliation;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.payout.api.query.reconciliation.GetPayoutSummaryForDrawQuery;
import com.tchalanet.server.core.payout.api.query.reconciliation.PayoutSummaryForDrawRow;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimStatus;
import com.tchalanet.server.core.payout.internal.infra.persistence.PayoutJpaEntity;
import com.tchalanet.server.core.payout.internal.infra.persistence.SpringPayoutJpaRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPayoutSummaryForDrawQueryHandler
    implements QueryHandler<GetPayoutSummaryForDrawQuery, PayoutSummaryForDrawRow> {

    private final QueryBus queryBus;
    private final SpringPayoutJpaRepository repository;

    @Override
    public PayoutSummaryForDrawRow handle(GetPayoutSummaryForDrawQuery query) {
        var tickets = PayoutReconciliationRows.ticketRows(queryBus, query.drawId());
        var payouts = repository.findByTicketIdIn(tickets.keySet().stream().map(ticketId -> ticketId.value()).toList());
        var currency = payouts.isEmpty() ? CurrencyCode.of("HTG") : CurrencyCode.of(payouts.getFirst().getCurrency());
        return new PayoutSummaryForDrawRow(
            query.drawId(),
            payouts.size(),
            payouts.stream().filter(p -> p.getStatus() == PayoutClaimStatus.OPEN).count(),
            payouts.stream().filter(p -> p.getStatus() == PayoutClaimStatus.CANCELLED).count(),
            payouts.stream().filter(p -> p.getStatus() == PayoutClaimStatus.PAID).count(),
            new Money(sum(payouts, false), currency),
            new Money(sum(payouts, true), currency)
        );
    }

    private static BigDecimal sum(java.util.List<PayoutJpaEntity> payouts, boolean paidOnly) {
        return payouts.stream()
            .filter(payout -> !paidOnly || payout.getStatus() == PayoutClaimStatus.PAID)
            .map(payout -> BigDecimal.valueOf(payout.getAmountCents(), 2))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
