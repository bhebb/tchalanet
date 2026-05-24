package com.tchalanet.server.core.offlinesync.internal.domain.model.submission;

import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.OfflineSyncBatchId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PromotionAttemptId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.offlinesync.api.model.submission.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.api.model.submission.SubmissionLifecycle;
import com.tchalanet.server.core.offlinesync.api.model.submission.SubmissionPromotionTrace;

import java.time.Instant;
import java.util.UUID;

/**
 * OfflineSubmission — Aggregate root.
 *
 * <p>Server-side representation of an offline sale before promotionDecision to a {@code Ticket}.
 * Composed of {@link SubmissionIdentity}, {@link SubmissionContext}, {@link SubmissionPayload},
 * {@link SubmissionLifecycle} and {@link SubmissionPromotionTrace}.
 */
public record OfflineSubmission(
    SubmissionIdentity identity,
    SubmissionContext context,
    SubmissionPayload payload,
    SubmissionLifecycle lifecycle,
    SubmissionPromotionTrace promotion
) {

    public OfflineSubmission {
        if (identity == null) throw new IllegalArgumentException("identity required");
        if (context == null) throw new IllegalArgumentException("context required");
        if (payload == null) throw new IllegalArgumentException("payload required");
        if (lifecycle == null) throw new IllegalArgumentException("lifecycle required");
        if (promotion == null) throw new IllegalArgumentException("promotionDecision required");
    }

    public static OfflineSubmission receive(
        OfflineSubmissionId id, TenantId tenantId,
        OfflineSyncBatchId syncBatchId, OfflineGrantId grantId, OfflineCodeBatchId codeBatchId,
        String offlineCode, String clientSubmissionId,
        UUID deviceId, UserId sellerUserId,
        TerminalId terminalId, OutletId outletId, SalesSessionId salesSessionId,
        com.tchalanet.server.common.types.id.DrawId drawId,
        Instant clientSoldAt, Instant receivedAt,
        Money totalStakeAmount, int lineCount,
        String payloadHash, String signature
    ) {
        return new OfflineSubmission(
            new SubmissionIdentity(id, tenantId, syncBatchId, grantId, codeBatchId, offlineCode, clientSubmissionId),
            new SubmissionContext(deviceId, sellerUserId, terminalId, outletId, salesSessionId),
            new SubmissionPayload(drawId, clientSoldAt, totalStakeAmount, lineCount, payloadHash, signature),
            SubmissionLifecycle.received(receivedAt),
            SubmissionPromotionTrace.empty()
        );
    }

    public OfflineSubmission markTechValidated(PromotionAttemptId attemptId, Instant now) {
        requireStatus(OfflineSubmissionStatus.RECEIVED);
        if (attemptId == null) throw new IllegalArgumentException("attemptId required");
        return new OfflineSubmission(
            identity, context, payload,
            lifecycle.transitionTo(OfflineSubmissionStatus.TECH_VALIDATED, now),
            promotion.withAttempt(attemptId, now)
        );
    }

    public OfflineSubmission markTechRejected(String code, String reason, Instant now) {
        requireStatus(OfflineSubmissionStatus.RECEIVED);
        return new OfflineSubmission(
            identity, context, payload,
            lifecycle.reject(OfflineSubmissionStatus.TECH_REJECTED, code, reason, now),
            promotion
        );
    }

    public OfflineSubmission markPromotionRequested(PromotionAttemptId attemptId, Instant now) {
        if (lifecycle.status() != OfflineSubmissionStatus.TECH_VALIDATED
            && lifecycle.status() != OfflineSubmissionStatus.SYNC_FAILED) {
            throw new IllegalStateException(
                "submission " + identity.id() + " cannot request promotionDecision from " + lifecycle.status());
        }
        return new OfflineSubmission(
            identity, context, payload,
            lifecycle.transitionTo(OfflineSubmissionStatus.PROMOTION_REQUESTED, lifecycle.processedAt()),
            promotion.withAttempt(attemptId, now)
        );
    }

    public OfflineSubmission markPromoted(TicketId ticketId, EventId eventId, Instant now) {
        requireStatus(OfflineSubmissionStatus.PROMOTION_REQUESTED, OfflineSubmissionStatus.TECH_VALIDATED);
        return new OfflineSubmission(
            identity, context, payload,
            lifecycle.transitionTo(OfflineSubmissionStatus.PROMOTED, now),
            promotion.withCreatedTicket(ticketId, eventId)
        );
    }

    public OfflineSubmission markBusinessRejected(String code, String reason, EventId eventId, Instant now) {
        requireStatus(OfflineSubmissionStatus.PROMOTION_REQUESTED, OfflineSubmissionStatus.TECH_VALIDATED);
        return new OfflineSubmission(
            identity, context, payload,
            lifecycle.reject(OfflineSubmissionStatus.BUSINESS_REJECTED, code, reason, now),
            promotion.withReturnEvent(eventId)
        );
    }

    public OfflineSubmission markNeedsReview(String reason, Instant now) {
        return new OfflineSubmission(
            identity, context, payload,
            lifecycle.reject(OfflineSubmissionStatus.NEEDS_ADMIN_REVIEW, "admin.review_required", reason, now),
            promotion
        );
    }

    public OfflineSubmission markAdminApproved(Instant now) {
        requireStatus(OfflineSubmissionStatus.NEEDS_ADMIN_REVIEW, OfflineSubmissionStatus.BUSINESS_REJECTED);
        return new OfflineSubmission(
            identity, context, payload,
            lifecycle.transitionTo(OfflineSubmissionStatus.ADMIN_APPROVED, now),
            promotion
        );
    }

    public OfflineSubmission markAdminRejected(String reason, Instant now) {
        requireStatus(OfflineSubmissionStatus.NEEDS_ADMIN_REVIEW, OfflineSubmissionStatus.BUSINESS_REJECTED);
        return new OfflineSubmission(
            identity, context, payload,
            lifecycle.reject(OfflineSubmissionStatus.ADMIN_REJECTED, "admin.rejected", reason, now),
            promotion
        );
    }

    public OfflineSubmission markSyncFailed(String code, String reason, Instant now) {
        return new OfflineSubmission(
            identity, context, payload,
            lifecycle.reject(OfflineSubmissionStatus.SYNC_FAILED, code, reason, now),
            promotion
        );
    }

    public boolean isCurrentPromotionAttempt(PromotionAttemptId incoming) {
        return promotion.isCurrentAttempt(incoming);
    }

    private void requireStatus(OfflineSubmissionStatus... allowed) {
        for (OfflineSubmissionStatus s : allowed) {
            if (lifecycle.status() == s) return;
        }
        throw new IllegalStateException(
            "submission " + identity.id() + " status " + lifecycle.status() + " not in expected set");
    }

    // Convenience accessors
    public OfflineSubmissionId id() { return identity.id(); }
    public OfflineSubmissionStatus status() { return lifecycle.status(); }
    public PromotionAttemptId promotionAttemptId() { return promotion.promotionAttemptId(); }
}
