package com.tchalanet.server.core.offlinesync.internal.domain.model;

import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record OfflineSalesGrant(
    OfflineSalesGrantId id,
    TenantId tenantId,
    TerminalId terminalId,
    OutletId outletId,
    UserId issuedTo,
    OfflineSalesGrantStatus status,
    Instant issuedAt,
    Instant expiresAt,
    Instant revokedAt,
    String revokedReason
) {
    public boolean isActive(Instant now) {
        return status == OfflineSalesGrantStatus.ACTIVE && expiresAt.isAfter(now);
    }

    public OfflineSalesGrant revoke(String reason, Instant now) {
        if (status != OfflineSalesGrantStatus.ACTIVE) {
            throw new IllegalStateException("Grant is not active");
        }
        return new OfflineSalesGrant(id, tenantId, terminalId, outletId, issuedTo,
            OfflineSalesGrantStatus.REVOKED, issuedAt, expiresAt, now, reason);
    }
}
