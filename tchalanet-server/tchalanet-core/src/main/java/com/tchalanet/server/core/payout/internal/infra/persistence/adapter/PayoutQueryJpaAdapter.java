package com.tchalanet.server.core.payout.internal.infra.persistence.adapter;

import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.core.payout.api.query.ListPayoutsQuery;
import com.tchalanet.server.core.payout.api.query.PayoutDetails;
import com.tchalanet.server.core.payout.api.query.PayoutReceiptView;
import com.tchalanet.server.core.payout.api.query.PayoutRow;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutQueryReaderPort;
import com.tchalanet.server.core.payout.internal.infra.persistence.PayoutJpaEntity;
import com.tchalanet.server.core.payout.internal.infra.persistence.SpringPayoutJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PayoutQueryJpaAdapter implements PayoutQueryReaderPort {

    private final SpringPayoutJpaRepository jpaRepo;

    @Override
    public TchPage<PayoutRow> list(ListPayoutsQuery query) {
        var page = jpaRepo.search(
            query.status() == null ? null : query.status().name(),
            query.ticketId() == null ? null : query.ticketId().value(),
            query.outletId() == null ? null : query.outletId().value(),
            query.sessionId() == null ? null : query.sessionId().value(),
            query.from(),
            query.to(),
            query.pageable());

        return TchPageMapper.map(page, this::toRow);
    }

    @Override
    public PayoutDetails getDetailsById(PayoutId payoutId) {
        return jpaRepo.findById(payoutId.value())
            .map(this::toDetails)
            .orElseThrow(() -> new TchNotFoundException(payoutId.toString(), "Payout not found: "));
    }

    @Override
    public PayoutReceiptView getReceiptViewById(PayoutId payoutId) {
        return jpaRepo.findById(payoutId.value())
            .map(this::toReceiptView)
            .orElseThrow(() -> new TchNotFoundException(payoutId.toString(), "Payout receipt not found: "));
    }

    private PayoutReceiptView toReceiptView(PayoutJpaEntity e) {
        return new PayoutReceiptView(
            TenantId.of(e.getTenantId()),
            PayoutId.of(e.getId()),
            TicketId.of(e.getTicketId()),
            centsToAmount(e.getAmountCents()),
            e.getCurrency(),
            e.getPaidAt(),
            OutletId.nullableOf(e.getPayingOutletId()),
            SalesSessionId.nullableOf(e.getPayingSessionId()),
            TerminalId.nullableOf(e.getPayingTerminalId()),
            null
        );
    }

    private static BigDecimal centsToAmount(Long cents) {
        return cents == null ? BigDecimal.ZERO : BigDecimal.valueOf(cents, 2);
    }

    private PayoutRow toRow(PayoutJpaEntity e) {
        return new PayoutRow(
            PayoutId.of(e.getId()),
            TicketId.of(e.getTicketId()),
            centsToAmount(e.getAmountCents()),
            e.getStatus(),
            e.getOpenedAt(),
            OutletId.nullableOf(
                e.getPayingOutletId() != null
                    ? e.getPayingOutletId()
                    : e.getSellingOutletId()),
            null
        );
    }

    private PayoutDetails toDetails(PayoutJpaEntity e) {
        return new PayoutDetails(
            PayoutId.of(e.getId()),
            TicketId.of(e.getTicketId()),
            e.getDrawId() != null ? DrawId.of(e.getDrawId()) : null,
            centsToAmount(e.getAmountCents()),
            e.getStatus(),
            e.getSource(),

            OutletId.nullableOf(
                e.getPayingOutletId() != null
                    ? e.getPayingOutletId()
                    : e.getSellingOutletId()),
            null,

            SalesSessionId.nullableOf(
                e.getPayingSessionId() != null
                    ? e.getPayingSessionId()
                    : e.getSellingSessionId()),

            TerminalId.nullableOf(e.getPayingTerminalId()),

            UserId.nullableOf(e.getPaidBy()),
            UserId.nullableOf(e.getBlockedBy()),
            UserId.nullableOf(e.getCancelledBy()),
            UserId.nullableOf(e.getReversedBy()),

            e.getOpenedAt(),
            e.getPaidAt(),
            e.getBlockedAt(),
            e.getCancelledAt(),
            e.getReversedAt(),

            e.getBlockReason(),
            e.getCancelReason(),
            e.getReverseReason()
        );
    }
}
