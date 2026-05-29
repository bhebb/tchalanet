package com.tchalanet.server.platform.entitlement.api.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

public record TenantPlanSnapshot(
    TenantId tenantId,
    String planCode,
    TenantPlanStatus status,
    Instant startsAt,
    Instant endsAt
) {
    public boolean activeAt(Instant now) {
        return status == TenantPlanStatus.ACTIVE
            && (startsAt == null || !startsAt.isAfter(now))
            && (endsAt == null || endsAt.isAfter(now));
    }
}
