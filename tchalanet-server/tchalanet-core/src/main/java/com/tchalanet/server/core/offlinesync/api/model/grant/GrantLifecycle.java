package com.tchalanet.server.core.offlinesync.api.model.grant;

import java.time.Instant;

/**
 * Lifecycle trace of an {@code OfflineGrant}: current status + audit timestamps.
 */
public record GrantLifecycle(
    OfflineGrantStatus status,
    Instant issuedAt,
    Instant revokedAt,
    String revokedReason
) {
    public GrantLifecycle {
        if (status == null) throw new IllegalArgumentException("status required");
        if (issuedAt == null) throw new IllegalArgumentException("issuedAt required");
        if (status == OfflineGrantStatus.REVOKED && (revokedAt == null || revokedReason == null))
            throw new IllegalArgumentException("revokedAt and revokedReason required when REVOKED");
    }

    public static GrantLifecycle issued(Instant issuedAt) {
        return new GrantLifecycle(OfflineGrantStatus.ACTIVE, issuedAt, null, null);
    }

    public GrantLifecycle revoked(String reason, Instant now) {
        if (status != OfflineGrantStatus.ACTIVE)
            throw new IllegalStateException("cannot revoke from status " + status);
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("revoke reason required");
        return new GrantLifecycle(OfflineGrantStatus.REVOKED, issuedAt, now, reason);
    }

    public GrantLifecycle expired() {
        if (status != OfflineGrantStatus.ACTIVE) return this;
        return new GrantLifecycle(OfflineGrantStatus.EXPIRED, issuedAt, revokedAt, revokedReason);
    }
}
