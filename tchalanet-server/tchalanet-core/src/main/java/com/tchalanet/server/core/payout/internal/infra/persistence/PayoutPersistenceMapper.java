package com.tchalanet.server.core.payout.internal.infra.persistence;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.payout.internal.domain.model.Payout;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutStatus;
import org.springframework.stereotype.Component;

@Component
public class PayoutPersistenceMapper {

    public Payout toDomain(PayoutJpaEntity e) {
        return new Payout(
            PayoutId.of(e.getId()),
            TenantId.of(e.getTenantId()),
            TicketId.of(e.getTicketId()),
            e.getAmountCents(),
            e.getCurrency(),
            e.getStatus(),
            OutletId.nullableOf(e.getSellingOutletId()),
            SalesSessionId.nullableOf(e.getSellingSessionId()),
            OutletId.nullableOf(e.getPayingOutletId()),
            SalesSessionId.nullableOf(e.getPayingSessionId()),
            TerminalId.nullableOf(e.getPayingTerminalId()),
            UserId.nullableOf(e.getRequestedBy()),
            e.getCreatedAt(),
            UserId.nullableOf(e.getApprovedBy()),
            e.getApprovedAt(),
            UserId.nullableOf(e.getRejectedBy()),
            e.getRejectedAt(),
            e.getRejectedReason(),
            UserId.nullableOf(e.getPaidBy()),
            e.getPaidAt(),
            null,
            null,
            null,
            e.getReason());
    }

    public void updateEntity(Payout p, PayoutJpaEntity e) {
        if (p.id() != null) e.setId(p.id().value());
        if (p.tenantId() != null) e.setTenantId(p.tenantId().value());
        if (p.ticketId() != null) e.setTicketId(p.ticketId().value());

        e.setAmountCents(p.amountCents());
        e.setCurrency(p.currency());
        e.setStatus(p.status());

        e.setSellingOutletId(p.sellingOutletId() == null ? null : p.sellingOutletId().value());
        e.setSellingSessionId(p.sellingSessionId() == null ? null : p.sellingSessionId().value());

        e.setPayingOutletId(p.payingOutletId() == null ? null : p.payingOutletId().value());
        e.setPayingSessionId(p.payingSessionId() == null ? null : p.payingSessionId().value());
        e.setPayingTerminalId(p.payingTerminalId() == null ? null : p.payingTerminalId().value());

        e.setRequestedBy(p.requestedBy() == null ? null : p.requestedBy().value());
        e.setApprovedBy(p.approvedBy() == null ? null : p.approvedBy().value());
        e.setRejectedBy(p.rejectedBy() == null ? null : p.rejectedBy().value());
        e.setPaidBy(p.paidBy() == null ? null : p.paidBy().value());

        e.setApprovedAt(p.approvedAt());
        e.setRejectedAt(p.rejectedAt());
        e.setPaidAt(p.paidAt());

        e.setRejectedReason(p.rejectedReason());
        e.setReason(p.reason());
    }

    public PayoutJpaEntity toNewEntity(Payout p) {
        var e = new PayoutJpaEntity();
        updateEntity(p, e);
        return e;
    }
}
