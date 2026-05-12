package com.tchalanet.server.core.payout.internal.domain.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record Payout(
    PayoutId id,
    TenantId tenantId,
    TicketId ticketId,
    Long amountCents,
    String currency,
    PayoutStatus status,
    OutletId sellingOutletId,
    SalesSessionId sellingSessionId,
    OutletId payingOutletId,
    SalesSessionId payingSessionId,
    TerminalId payingTerminalId,
    UserId requestedBy,
    Instant requestedAt,
    UserId approvedBy,
    Instant approvedAt,
    UserId rejectedBy,
    Instant rejectedAt,
    String rejectedReason,
    UserId paidBy,
    Instant paidAt,
    UserId cancelledBy,
    Instant cancelledAt,
    String cancelReason,
    String reason) {

    public Payout approve(UserId approvedBy, Instant approvedAt) {
        if (status != PayoutStatus.REQUESTED) {
            throw new IllegalStateException("Only requested payout can be approved");
        }

        return new Payout(
            id, tenantId, ticketId, amountCents, currency, PayoutStatus.APPROVED,
            sellingOutletId, sellingSessionId, payingOutletId, payingSessionId, payingTerminalId,
            requestedBy, requestedAt, approvedBy, approvedAt, rejectedBy, rejectedAt, rejectedReason,
            paidBy, paidAt, cancelledBy, cancelledAt, cancelReason, reason);
    }

    public Payout pay(
        OutletId payingOutletId,
        SalesSessionId payingSessionId,
        TerminalId payingTerminalId,
        UserId paidBy,
        Instant paidAt) {

        if (status != PayoutStatus.APPROVED && status != PayoutStatus.REQUESTED) {
            throw new IllegalStateException("Only requested or approved payout can be paid");
        }

        return new Payout(
            id, tenantId, ticketId, amountCents, currency, PayoutStatus.PAID,
            sellingOutletId, sellingSessionId, payingOutletId, payingSessionId, payingTerminalId,
            requestedBy, requestedAt, approvedBy, approvedAt, rejectedBy, rejectedAt, rejectedReason,
            paidBy, paidAt, cancelledBy, cancelledAt, cancelReason, reason);
    }

    public Payout reject(UserId rejectedBy, Instant rejectedAt, String rejectedReason) {
        if (status == PayoutStatus.PAID || status == PayoutStatus.CANCELLED) {
            throw new IllegalStateException("Paid or cancelled payout cannot be rejected");
        }

        return new Payout(
            id, tenantId, ticketId, amountCents, currency, PayoutStatus.REJECTED,
            sellingOutletId, sellingSessionId, payingOutletId, payingSessionId, payingTerminalId,
            requestedBy, requestedAt, approvedBy, approvedAt, rejectedBy, rejectedAt, rejectedReason,
            paidBy, paidAt, cancelledBy, cancelledAt, cancelReason, reason);
    }

    public Payout cancel(UserId cancelledBy, Instant cancelledAt, String cancelReason) {
        if (status == PayoutStatus.PAID) {
            throw new IllegalStateException("Paid payout cannot be cancelled");
        }

        return new Payout(
            id, tenantId, ticketId, amountCents, currency, PayoutStatus.CANCELLED,
            sellingOutletId, sellingSessionId, payingOutletId, payingSessionId, payingTerminalId,
            requestedBy, requestedAt, approvedBy, approvedAt, rejectedBy, rejectedAt, rejectedReason,
            paidBy, paidAt, cancelledBy, cancelledAt, cancelReason, reason);
    }
}
