package com.tchalanet.server.core.offlinesync.api.model.codebatch;

import java.time.Instant;

/** Lifecycle trace of an offline code batch. */
public record CodeBatchLifecycle(
    OfflineCodeBatchStatus status,
    Instant issuedAt,
    Instant expiresAt
) {
    public CodeBatchLifecycle {
        if (status == null) throw new IllegalArgumentException("status required");
        if (issuedAt == null || expiresAt == null)
            throw new IllegalArgumentException("issuedAt and expiresAt required");
        if (issuedAt.isAfter(expiresAt))
            throw new IllegalArgumentException("issuedAt must be <= expiresAt");
    }

    public static CodeBatchLifecycle active(Instant issuedAt, Instant expiresAt) {
        return new CodeBatchLifecycle(OfflineCodeBatchStatus.ACTIVE, issuedAt, expiresAt);
    }

    public CodeBatchLifecycle expired() {
        return new CodeBatchLifecycle(OfflineCodeBatchStatus.EXPIRED, issuedAt, expiresAt);
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
