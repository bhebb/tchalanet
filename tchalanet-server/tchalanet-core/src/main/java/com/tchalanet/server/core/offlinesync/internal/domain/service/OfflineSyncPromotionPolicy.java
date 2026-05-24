package com.tchalanet.server.core.offlinesync.internal.domain.service;

import com.tchalanet.server.common.types.id.PromotionAttemptId;
import com.tchalanet.server.core.offlinesync.api.model.submission.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.internal.domain.model.submission.OfflineSubmission;

/**
 * Pure policy deciding whether a returning {@code OfflineSubmissionProcessedEvent} should be
 * applied to a submission, or ignored as stale.
 */
public final class OfflineSyncPromotionPolicy {

    private OfflineSyncPromotionPolicy() {}

    public sealed interface Outcome {
        record Apply() implements Outcome {}
        record Ignore(String reason) implements Outcome {}
    }

    public static Outcome evaluateReturn(OfflineSubmission submission, PromotionAttemptId incoming) {
        if (incoming == null) {
            return new Outcome.Ignore("missing promotionAttemptId");
        }
        if (!submission.isCurrentPromotionAttempt(incoming)) {
            return new Outcome.Ignore("stale promotionDecision attempt: incoming=" + incoming
                + " current=" + submission.promotionAttemptId());
        }
        if (submission.status() != OfflineSubmissionStatus.PROMOTION_REQUESTED
            && submission.status() != OfflineSubmissionStatus.TECH_VALIDATED) {
            return new Outcome.Ignore("submission already in terminal status " + submission.status());
        }
        return new Outcome.Apply();
    }
}
