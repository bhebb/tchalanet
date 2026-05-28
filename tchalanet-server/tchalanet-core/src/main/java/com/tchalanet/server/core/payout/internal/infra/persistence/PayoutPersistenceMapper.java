package com.tchalanet.server.core.payout.internal.infra.persistence;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaim;
import org.springframework.stereotype.Component;

@Component
public class PayoutPersistenceMapper {

    public PayoutClaim toDomain(PayoutJpaEntity e) {
        return new PayoutClaim(
            PayoutId.of(e.getId()),
            TenantId.of(e.getTenantId()),
            TicketId.of(e.getTicketId()),
            e.getDrawId() != null ? DrawId.of(e.getDrawId()) : null,
            e.getAmountCents(),
            e.getCurrency(),
            e.getStatus(),
            e.getSource(),
            e.getSourceEventId() != null ? EventId.of(e.getSourceEventId()) : null,
            OutletId.nullableOf(e.getSellingOutletId()),
            SalesSessionId.nullableOf(e.getSellingSessionId()),
            e.getOpenedAt(),
            OutletId.nullableOf(e.getPayingOutletId()),
            SalesSessionId.nullableOf(e.getPayingSessionId()),
            TerminalId.nullableOf(e.getPayingTerminalId()),
            UserId.nullableOf(e.getPaidBy()),
            e.getPaidAt(),
            UserId.nullableOf(e.getBlockedBy()),
            e.getBlockedAt(),
            e.getBlockReason(),
            UserId.nullableOf(e.getCancelledBy()),
            e.getCancelledAt(),
            e.getCancelReason(),
            UserId.nullableOf(e.getReversedBy()),
            e.getReversedAt(),
            e.getReverseReason());
    }

    public void updateEntity(PayoutClaim p, PayoutJpaEntity e) {
        if (p.id() != null) e.setId(p.id().value());
        if (p.tenantId() != null) e.setTenantId(p.tenantId().value());
        if (p.ticketId() != null) e.setTicketId(p.ticketId().value());

        e.setDrawId(p.drawId() != null ? p.drawId().value() : null);
        e.setAmountCents(p.amountCents());
        e.setCurrency(p.currency());
        e.setStatus(p.status());
        e.setSource(p.source());
        e.setSourceEventId(p.sourceEventId() != null ? p.sourceEventId().value() : null);

        e.setSellingOutletId(p.sellingOutletId() == null ? null : p.sellingOutletId().value());
        e.setSellingSessionId(p.sellingSessionId() == null ? null : p.sellingSessionId().value());
        e.setOpenedAt(p.openedAt());

        e.setPayingOutletId(p.payingOutletId() == null ? null : p.payingOutletId().value());
        e.setPayingSessionId(p.payingSessionId() == null ? null : p.payingSessionId().value());
        e.setPayingTerminalId(p.payingTerminalId() == null ? null : p.payingTerminalId().value());

        e.setPaidBy(p.paidBy() == null ? null : p.paidBy().value());
        e.setPaidAt(p.paidAt());

        e.setBlockedBy(p.blockedBy() == null ? null : p.blockedBy().value());
        e.setBlockedAt(p.blockedAt());
        e.setBlockReason(p.blockReason());

        e.setCancelledBy(p.cancelledBy() == null ? null : p.cancelledBy().value());
        e.setCancelledAt(p.cancelledAt());
        e.setCancelReason(p.cancelReason());

        e.setReversedBy(p.reversedBy() == null ? null : p.reversedBy().value());
        e.setReversedAt(p.reversedAt());
        e.setReverseReason(p.reverseReason());
    }

    public PayoutJpaEntity toNewEntity(PayoutClaim p) {
        var e = new PayoutJpaEntity();
        updateEntity(p, e);
        return e;
    }
}
