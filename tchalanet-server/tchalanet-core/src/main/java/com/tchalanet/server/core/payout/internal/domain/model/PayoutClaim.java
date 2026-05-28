package com.tchalanet.server.core.payout.internal.domain.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;

public record PayoutClaim(
    PayoutId id,
    TenantId tenantId,
    TicketId ticketId,
    DrawId drawId,
    Long amountCents,
    String currency,
    PayoutClaimStatus status,
    PayoutClaimSource source,
    EventId sourceEventId,
    OutletId sellingOutletId,
    SalesSessionId sellingSessionId,
    Instant openedAt,
    OutletId payingOutletId,
    SalesSessionId payingSessionId,
    TerminalId payingTerminalId,
    UserId paidBy,
    Instant paidAt,
    UserId blockedBy,
    Instant blockedAt,
    String blockReason,
    UserId cancelledBy,
    Instant cancelledAt,
    String cancelReason,
    UserId reversedBy,
    Instant reversedAt,
    String reverseReason) {

    public PayoutClaim pay(
            OutletId payingOutletId,
            SalesSessionId payingSessionId,
            TerminalId payingTerminalId,
            UserId paidBy,
            Instant paidAt) {
        if (status != PayoutClaimStatus.OPEN) {
            throw new IllegalStateException("Only an OPEN claim can be paid, current status: " + status);
        }
        return new PayoutClaim(
            id, tenantId, ticketId, drawId, amountCents, currency, PayoutClaimStatus.PAID, source, sourceEventId,
            sellingOutletId, sellingSessionId, openedAt,
            payingOutletId, payingSessionId, payingTerminalId,
            paidBy, paidAt,
            blockedBy, blockedAt, blockReason,
            cancelledBy, cancelledAt, cancelReason,
            reversedBy, reversedAt, reverseReason);
    }

    public PayoutClaim block(UserId blockedBy, Instant blockedAt, String blockReason) {
        if (status != PayoutClaimStatus.OPEN) {
            throw new IllegalStateException("Only an OPEN claim can be blocked, current status: " + status);
        }
        return new PayoutClaim(
            id, tenantId, ticketId, drawId, amountCents, currency, PayoutClaimStatus.BLOCKED, source, sourceEventId,
            sellingOutletId, sellingSessionId, openedAt,
            payingOutletId, payingSessionId, payingTerminalId,
            paidBy, paidAt,
            blockedBy, blockedAt, blockReason,
            cancelledBy, cancelledAt, cancelReason,
            reversedBy, reversedAt, reverseReason);
    }

    public PayoutClaim unblock(Instant now) {
        if (status != PayoutClaimStatus.BLOCKED) {
            throw new IllegalStateException("Only a BLOCKED claim can be unblocked, current status: " + status);
        }
        return new PayoutClaim(
            id, tenantId, ticketId, drawId, amountCents, currency, PayoutClaimStatus.OPEN, source, sourceEventId,
            sellingOutletId, sellingSessionId, openedAt,
            payingOutletId, payingSessionId, payingTerminalId,
            paidBy, paidAt,
            null, null, null,
            cancelledBy, cancelledAt, cancelReason,
            reversedBy, reversedAt, reverseReason);
    }

    public PayoutClaim cancel(UserId cancelledBy, Instant cancelledAt, String cancelReason) {
        if (status == PayoutClaimStatus.PAID || status == PayoutClaimStatus.REVERSED) {
            throw new IllegalStateException("Cannot cancel a claim with status: " + status);
        }
        return new PayoutClaim(
            id, tenantId, ticketId, drawId, amountCents, currency, PayoutClaimStatus.CANCELLED, source, sourceEventId,
            sellingOutletId, sellingSessionId, openedAt,
            payingOutletId, payingSessionId, payingTerminalId,
            paidBy, paidAt,
            blockedBy, blockedAt, blockReason,
            cancelledBy, cancelledAt, cancelReason,
            reversedBy, reversedAt, reverseReason);
    }

    public PayoutClaim reverse(UserId reversedBy, Instant reversedAt, String reverseReason) {
        if (status != PayoutClaimStatus.PAID) {
            throw new IllegalStateException("Only a PAID claim can be reversed, current status: " + status);
        }
        return new PayoutClaim(
            id, tenantId, ticketId, drawId, amountCents, currency, PayoutClaimStatus.REVERSED, source, sourceEventId,
            sellingOutletId, sellingSessionId, openedAt,
            payingOutletId, payingSessionId, payingTerminalId,
            paidBy, paidAt,
            blockedBy, blockedAt, blockReason,
            cancelledBy, cancelledAt, cancelReason,
            reversedBy, reversedAt, reverseReason);
    }
}
