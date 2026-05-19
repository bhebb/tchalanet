package com.tchalanet.server.core.offlinesync.api.model.submission;

import java.time.Instant;

/**
 * Lifecycle trace of an {@code OfflineSubmission}: status + processing timestamps + rejection.
 */
public record SubmissionLifecycle(
    OfflineSubmissionStatus status,
    Instant receivedAt,
    Instant processedAt,
    String rejectionCode,
    String rejectionReason
) {
    public SubmissionLifecycle {
        if (status == null) throw new IllegalArgumentException("status required");
        if (receivedAt == null) throw new IllegalArgumentException("receivedAt required");
    }

    public static SubmissionLifecycle received(Instant receivedAt) {
        return new SubmissionLifecycle(OfflineSubmissionStatus.RECEIVED, receivedAt, null, null, null);
    }

    public SubmissionLifecycle transitionTo(OfflineSubmissionStatus next, Instant processedAt) {
        return new SubmissionLifecycle(next, receivedAt, processedAt, null, null);
    }

    public SubmissionLifecycle reject(OfflineSubmissionStatus next, String code, String reason, Instant processedAt) {
        if (code == null || code.isBlank() || reason == null || reason.isBlank())
            throw new IllegalArgumentException("rejection code and reason required");
        return new SubmissionLifecycle(next, receivedAt, processedAt, code, reason);
    }
}
