package com.tchalanet.server.core.payout.internal.infra.persistence;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.payout.domain.model.Payout;
import com.tchalanet.server.core.payout.domain.model.PayoutStatus;
import org.springframework.stereotype.Component;

@Component
public class PayoutPersistenceMapper {

    public Payout toDomain(PayoutJpaEntity e) {
        return Payout.load(
            PayoutId.of(e.getId()),
            TenantId.of(e.getTenantId()),
            TicketId.of(e.getTicketId()),
            e.getAmountCents(),
            e.getCurrency(),
            OutletId.nullableOf(e.getSellingOutletId()),
            SalesSessionId.nullableOf(e.getSellingSessionId()),
            e.getCreatedAt(),
            PayoutStatus.valueOf(e.getStatus()),
            OutletId.nullableOf(e.getPayingOutletId()),
            SalesSessionId.nullableOf(e.getPayingSessionId()),
            TerminalId.nullableOf(e.getPayingTerminalId()),
            UserId.nullableOf(e.getRequestedBy()),
            UserId.nullableOf(e.getApprovedBy()),
            UserId.nullableOf(e.getRejectedBy()),
            UserId.nullableOf(e.getPaidBy()),
            e.getApprovedAt(),
            e.getRejectedAt(),
            e.getPaidAt(),
            e.getRejectedReason(),
            e.getReason());
    }

    public void updateEntity(Payout p, PayoutJpaEntity e) {
        if (p.getId() != null) e.setId(p.getId().value());
        if (p.getTenantId() != null) e.setTenantId(p.getTenantId().value());
        if (p.getTicketId() != null) e.setTicketId(p.getTicketId().value());

        e.setAmountCents(p.getAmountCents());
        e.setCurrency(p.getCurrency());
        e.setStatus(p.getStatus().name());

        e.setSellingOutletId(p.getSellingOutletId() == null ? null : p.getSellingOutletId().value());
        e.setSellingSessionId(p.getSellingSessionId() == null ? null : p.getSellingSessionId().value());

        e.setPayingOutletId(p.getPayingOutletId() == null ? null : p.getPayingOutletId().value());
        e.setPayingSessionId(p.getPayingSessionId() == null ? null : p.getPayingSessionId().value());
        e.setPayingTerminalId(p.getPayingTerminalId() == null ? null : p.getPayingTerminalId().value());

        e.setRequestedBy(p.getRequestedBy() == null ? null : p.getRequestedBy().value());
        e.setApprovedBy(p.getApprovedBy() == null ? null : p.getApprovedBy().value());
        e.setRejectedBy(p.getRejectedBy() == null ? null : p.getRejectedBy().value());
        e.setPaidBy(p.getPaidBy() == null ? null : p.getPaidBy().value());

        e.setApprovedAt(p.getApprovedAt());
        e.setRejectedAt(p.getRejectedAt());
        e.setPaidAt(p.getPaidAt());

        e.setRejectedReason(p.getRejectedReason());
        e.setReason(p.getReason());
    }

    public PayoutJpaEntity toNewEntity(Payout p) {
        var e = new PayoutJpaEntity();
        updateEntity(p, e);
        return e;
    }
}
