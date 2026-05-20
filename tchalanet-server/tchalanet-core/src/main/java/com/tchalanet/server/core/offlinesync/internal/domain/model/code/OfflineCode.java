package com.tchalanet.server.core.offlinesync.internal.domain.model.code;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.api.model.code.CodeLifecycle;
import com.tchalanet.server.core.offlinesync.api.model.code.OfflineCodeStatus;

import java.time.Instant;

/**
 * OfflineCode — Aggregate root.
 *
 * <p>Invariant: a code that has been submitted (i.e. transitioned to {@code RESERVED}) NEVER
 * returns to {@code AVAILABLE}. Technical rejection takes it to {@code CONSUMED_REJECTED};
 * accepted promotion takes it to {@code CONSUMED_PROMOTED}.
 */
public record OfflineCode(
    CodeIdentity identity,
    CodeLifecycle lifecycle
) {

    public OfflineCode {
        if (identity == null) throw new IllegalArgumentException("identity required");
        if (lifecycle == null) throw new IllegalArgumentException("lifecycle required");
    }

    public static OfflineCode issue(
        OfflineCodeId id, TenantId tenantId,
        OfflineCodeBatchId codeBatchId, OfflineGrantId grantId,
        String code, Instant expiresAt
    ) {
        return new OfflineCode(
            new CodeIdentity(id, tenantId, codeBatchId, grantId, code),
            CodeLifecycle.available(expiresAt)
        );
    }

    public OfflineCode reserve(OfflineSubmissionId submissionId, Instant now) {
        requireStatus(OfflineCodeStatus.AVAILABLE);
        if (lifecycle.isExpired(now))
            throw new IllegalStateException("code " + identity.id() + " is expired");
        return new OfflineCode(identity, lifecycle.reserved(submissionId, now));
    }

    public OfflineCode markConsumedPromoted(TicketId ticketId, Instant now) {
        requireStatus(OfflineCodeStatus.RESERVED);
        return new OfflineCode(identity, lifecycle.consumedPromoted(ticketId, now));
    }

    public OfflineCode markConsumedRejected(Instant now) {
        requireStatus(OfflineCodeStatus.RESERVED);
        return new OfflineCode(identity, lifecycle.consumedRejected(now));
    }

    public OfflineCode expire() {
        if (lifecycle.status() != OfflineCodeStatus.AVAILABLE) return this;
        return new OfflineCode(identity, lifecycle.expired());
    }

    public OfflineCode voidUnused() {
        requireStatus(OfflineCodeStatus.AVAILABLE);
        return new OfflineCode(identity, lifecycle.voided());
    }

    private void requireStatus(OfflineCodeStatus expected) {
        if (lifecycle.status() != expected) {
            String verb = expected == OfflineCodeStatus.AVAILABLE
                ? "cannot be reserved from status "
                : "must be " + expected + " (was ";
            String suffix = expected == OfflineCodeStatus.AVAILABLE
                ? lifecycle.status().toString()
                : lifecycle.status() + ")";
            throw new IllegalStateException("code " + identity.id() + " " + verb + suffix);
        }
    }

    // Convenience accessors
    public OfflineCodeId id() { return identity.id(); }
    public OfflineCodeBatchId codeBatchId() { return identity.codeBatchId(); }
    public OfflineCodeStatus status() { return lifecycle.status(); }
}
