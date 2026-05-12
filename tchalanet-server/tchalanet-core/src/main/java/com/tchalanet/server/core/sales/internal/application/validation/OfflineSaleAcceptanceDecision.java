package com.tchalanet.server.core.sales.internal.application.validation;

import com.tchalanet.server.core.offlinesync.domain.model.SalesOfflineDecision;
import com.tchalanet.server.core.offlinesync.domain.model.SalesOfflineRejectReason;

public record OfflineSaleAcceptanceDecision(
    SalesOfflineDecision decision,
    SalesOfflineRejectReason rejectReason
) {
    public boolean isAccepted() {
        return decision == SalesOfflineDecision.ACCEPTED;
    }

    public boolean requiresReview() {
        return decision == SalesOfflineDecision.REVIEW_REQUIRED;
    }

    public static OfflineSaleAcceptanceDecision accepted() {
        return new OfflineSaleAcceptanceDecision(SalesOfflineDecision.ACCEPTED, null);
    }

    public static OfflineSaleAcceptanceDecision rejected(SalesOfflineRejectReason reason) {
        return new OfflineSaleAcceptanceDecision(SalesOfflineDecision.REJECTED, reason);
    }

    public static OfflineSaleAcceptanceDecision reviewRequired(SalesOfflineRejectReason reason) {
        return new OfflineSaleAcceptanceDecision(SalesOfflineDecision.REVIEW_REQUIRED, reason);
    }
}
