package com.tchalanet.server.core.sales.internal.application.validation;

import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSaleSubmission;
import com.tchalanet.server.core.offlinesync.internal.domain.model.SalesOfflineDecision;
import com.tchalanet.server.core.offlinesync.internal.domain.model.SalesOfflineRejectReason;
import com.tchalanet.server.core.session.internal.domain.model.SalesSession;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Gates whether a technically-accepted offline submission can become an official ticket.
 * Enforces: FINALIZED session → REVIEW (§15), draw result known → REVIEW (§16),
 * device time alone → insufficient (§17).
 */
@Component
public class OfflineSaleAcceptanceValidator {

    public OfflineSaleAcceptanceDecision evaluate(
        OfflineSaleSubmission submission,
        SalesSession session,
        boolean drawResultKnown,
        Instant drawCutoff
    ) {
        // §15 — FINALIZED session must never auto-accept
        if (session.isFinalized()) {
            return OfflineSaleAcceptanceDecision.reviewRequired(
                SalesOfflineRejectReason.SESSION_FINALIZED);
        }

        // §16 — draw result already known at sync time
        if (drawResultKnown) {
            return OfflineSaleAcceptanceDecision.reviewRequired(
                SalesOfflineRejectReason.SYNC_AFTER_RESULT_KNOWN);
        }

        // §14 — draw cutoff already passed
        if (drawCutoff != null && submission.receivedAt().isAfter(drawCutoff)) {
            return OfflineSaleAcceptanceDecision.rejected(
                SalesOfflineRejectReason.DRAW_CUTOFF_PASSED);
        }

        return OfflineSaleAcceptanceDecision.accepted();
    }
}
