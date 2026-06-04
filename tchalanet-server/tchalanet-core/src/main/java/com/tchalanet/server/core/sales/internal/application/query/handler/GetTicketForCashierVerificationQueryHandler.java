package com.tchalanet.server.core.sales.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.model.verification.TicketCashierVerificationView;
import com.tchalanet.server.core.sales.api.query.GetTicketForCashierVerificationQuery;
import com.tchalanet.server.core.sales.internal.application.receipt.formatter.TicketPublicCodeFormatter;
import com.tchalanet.server.core.sales.internal.domain.service.CustomerTicketStatusResolver;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTicketForCashierVerificationQueryHandler
    implements QueryHandler<GetTicketForCashierVerificationQuery, TicketCashierVerificationView> {

    private final TicketJpaRepository repository;
    private final TicketPublicCodeFormatter publicCodeFormatter;
    private final CustomerTicketStatusResolver statusResolver;

    @Override
    public TicketCashierVerificationView handle(GetTicketForCashierVerificationQuery query) {
        // Canonicalize to the stored format: codes are stored WITH the dash
        // (e.g. "64A8-5KK3"). normalize() strips it → no match. display() strips
        // then re-inserts the dash, so both "64A8-5KK3" (typed) and "64A85KK3"
        // (from QR URL, which normalizes before embedding) resolve correctly.
        var publicCode = publicCodeFormatter.display(query.publicCode());
        var ticket = repository.findWithLinesByPublicCode(publicCode)
            .orElseThrow(() -> ProblemRest.notFound("ticket.not_found"));
        var currency = CurrencyCode.of(ticket.getCurrency());
        return new TicketCashierVerificationView(
            TicketId.of(ticket.getId()),
            ticket.getTicketCode(),
            ticket.getPublicCode(),
            publicCodeFormatter.display(ticket.getPublicCode()),
            statusResolver.resolve(ticket.getSaleStatus(), ticket.getResultStatus(), ticket.getSettlementStatus()),
            ticket.getSaleStatus(),
            ticket.getResultStatus(),
            ticket.getSettlementStatus(),
            new Money(ticket.getTotalAmount(), currency),
            new Money(ticket.getWinningAmount(), currency),
            ticket.getPlacedAt(),
            DrawId.of(ticket.getDrawId()),
            null
        );
    }
}
