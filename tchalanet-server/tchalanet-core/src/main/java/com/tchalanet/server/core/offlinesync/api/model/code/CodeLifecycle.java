package com.tchalanet.server.core.offlinesync.api.model.code;

import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TicketId;

import java.time.Instant;

/** Lifecycle trace of an {@code OfflineCode}: status + transition timestamps + references. */
public record CodeLifecycle(
    OfflineCodeStatus status,
    Instant reservedAt,
    Instant consumedAt,
    Instant expiresAt,
    OfflineSubmissionId offlineSubmissionId,
    TicketId ticketId
) {
    public CodeLifecycle {
        if (status == null) throw new IllegalArgumentException("status required");
        if (expiresAt == null) throw new IllegalArgumentException("expiresAt required");
    }

    public static CodeLifecycle available(Instant expiresAt) {
        return new CodeLifecycle(OfflineCodeStatus.AVAILABLE, null, null, expiresAt, null, null);
    }

    public CodeLifecycle reserved(OfflineSubmissionId submissionId, Instant now) {
        return new CodeLifecycle(OfflineCodeStatus.RESERVED, now, null, expiresAt, submissionId, null);
    }

    public CodeLifecycle consumedPromoted(TicketId ticketId, Instant now) {
        return new CodeLifecycle(OfflineCodeStatus.CONSUMED_PROMOTED, reservedAt, now, expiresAt,
            offlineSubmissionId, ticketId);
    }

    public CodeLifecycle consumedRejected(Instant now) {
        return new CodeLifecycle(OfflineCodeStatus.CONSUMED_REJECTED, reservedAt, now, expiresAt,
            offlineSubmissionId, null);
    }

    public CodeLifecycle expired() {
        return new CodeLifecycle(OfflineCodeStatus.EXPIRED, null, null, expiresAt, null, null);
    }

    public CodeLifecycle voided() {
        return new CodeLifecycle(OfflineCodeStatus.VOIDED, null, null, expiresAt, null, null);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
