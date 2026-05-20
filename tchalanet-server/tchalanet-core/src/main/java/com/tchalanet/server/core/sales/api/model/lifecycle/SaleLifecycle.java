package com.tchalanet.server.core.sales.api.model.lifecycle;


import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;

import java.time.Instant;

public record SaleLifecycle(
    TicketSaleStatus status,
    Instant soldAt,
    Instant placedAt,
    ApprovalTrace approval,
    DecisionTrace rejection,
    DecisionTrace cancellation,
    DecisionTrace voiding
) {
    public static SaleLifecycle initial(TicketSaleStatus status, Instant now) {
        return new SaleLifecycle(status, now, now, null, null, null, null);
    }

    public SaleLifecycle withApprovalRequest(
        ApprovalRequestId requestId, UserId requestedBy, Instant now
    ) {
        return new SaleLifecycle(
            status, soldAt, placedAt,
            new ApprovalTrace(requestId, requestedBy, now, null, null),
            rejection, cancellation, voiding
        );
    }

    public SaleLifecycle approved(UserId by, Instant now) {
        ApprovalTrace updated = (approval != null)
            ? new ApprovalTrace(approval.requestId(), approval.requestedBy(),
            approval.requestedAt(), now, by)
            : new ApprovalTrace(null, null, null, now, by);
        return new SaleLifecycle(
            TicketSaleStatus.APPROVED, soldAt, placedAt,
            updated, rejection, cancellation, voiding
        );
    }

    public SaleLifecycle rejected(UserId by, String reason, Instant now) {
        return new SaleLifecycle(
            TicketSaleStatus.REJECTED, soldAt, placedAt,
            approval, new DecisionTrace(now, by, reason), cancellation, voiding
        );
    }

    public SaleLifecycle cancelled(UserId by, String reason, Instant now) {
        return new SaleLifecycle(
            TicketSaleStatus.CANCELLED, soldAt, placedAt,
            approval, rejection, new DecisionTrace(now, by, reason), voiding
        );
    }

    public SaleLifecycle voided(UserId by, String reason, Instant now) {
        return new SaleLifecycle(
            TicketSaleStatus.VOIDED, soldAt, placedAt,
            approval, rejection, cancellation, new DecisionTrace(now, by, reason)
        );
    }
}
