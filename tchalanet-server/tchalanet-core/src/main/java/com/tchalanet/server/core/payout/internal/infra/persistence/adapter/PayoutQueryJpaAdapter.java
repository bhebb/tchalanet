package com.tchalanet.server.core.payout.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.NotFoundException;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.core.payout.api.query.ListPayoutsQuery;
import com.tchalanet.server.core.payout.api.query.PayoutDetails;
import com.tchalanet.server.core.payout.api.query.PayoutReceiptView;
import com.tchalanet.server.core.payout.api.query.PayoutRow;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutQueryReaderPort;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutStatus;
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
            .orElseThrow(() -> new NotFoundException("Payout not found: " + payoutId));
    }

    @Override
    public PayoutReceiptView getReceiptViewById(PayoutId payoutId) {
        return jpaRepo.findById(payoutId.value())
            .map(this::toReceiptView)
            .orElseThrow(() -> new NotFoundException("Payout receipt not found: " + payoutId));
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
            null // TODO resolve paidBy label via user/read model if needed
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
            e.getCreatedAt(),
            OutletId.nullableOf(
                e.getPayingOutletId() != null
                    ? e.getPayingOutletId()
                    : e.getSellingOutletId()),
            null // TODO outlet projection/join
        );
    }

    private PayoutDetails toDetails(PayoutJpaEntity e) {
        return new PayoutDetails(
            PayoutId.of(e.getId()),
            TicketId.of(e.getTicketId()),
            centsToAmount(e.getAmountCents()),
            e.getStatus(),

            OutletId.nullableOf(
                e.getPayingOutletId() != null
                    ? e.getPayingOutletId()
                    : e.getSellingOutletId()),
            null, // TODO outlet projection/join

            SalesSessionId.nullableOf(
                e.getPayingSessionId() != null
                    ? e.getPayingSessionId()
                    : e.getSellingSessionId()),

            TerminalId.nullableOf(e.getPayingTerminalId()),

            UserId.nullableOf(e.getRequestedBy()),
            UserId.nullableOf(e.getApprovedBy()),
            UserId.nullableOf(e.getPaidBy()),

            e.getCreatedAt(),
            e.getApprovedAt(),
            e.getPaidAt(),

            e.getRejectedReason(),
            e.getReason()
        );
    }
}
