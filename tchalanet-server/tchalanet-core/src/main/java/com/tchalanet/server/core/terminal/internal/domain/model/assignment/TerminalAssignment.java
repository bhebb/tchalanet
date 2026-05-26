package com.tchalanet.server.core.terminal.internal.domain.model.assignment;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalAssignmentId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import java.util.Objects;

public record TerminalAssignment(
    TerminalAssignmentId id,
    TenantId tenantId,
    TerminalId terminalId,
    UserId userId,
    TerminalAssignmentStatus status,
    Instant assignedAt,
    Instant revokedAt
) {

    public TerminalAssignment {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(terminalId, "terminalId is required");
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(assignedAt, "assignedAt is required");
        if (status == TerminalAssignmentStatus.REVOKED && revokedAt == null) {
            throw new IllegalArgumentException("revokedAt is required for revoked assignment");
        }
    }

    public static TerminalAssignment active(
        TerminalAssignmentId id,
        TenantId tenantId,
        TerminalId terminalId,
        UserId userId,
        Instant now
    ) {
        return new TerminalAssignment(
            id,
            tenantId,
            terminalId,
            userId,
            TerminalAssignmentStatus.ACTIVE,
            now,
            null
        );
    }

    public TerminalAssignment revoke(Instant now) {
        if (status == TerminalAssignmentStatus.REVOKED) {
            return this;
        }
        return new TerminalAssignment(
            id,
            tenantId,
            terminalId,
            userId,
            TerminalAssignmentStatus.REVOKED,
            assignedAt,
            Objects.requireNonNull(now, "now is required")
        );
    }

    public boolean activeFor(UserId actorUserId) {
        return status == TerminalAssignmentStatus.ACTIVE && userId.equals(actorUserId);
    }
}
