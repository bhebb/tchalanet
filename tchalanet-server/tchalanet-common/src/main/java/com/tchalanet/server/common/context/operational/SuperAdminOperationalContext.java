package com.tchalanet.server.common.context.operational;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record SuperAdminOperationalContext(
    TenantId effectiveTenantId,
    UserId actorUserId,
    String overrideReason,
    OperationalContextSource source,
    TrustLevel trustLevel
) implements OperationalRequestContext {

    @Override
    public OperationalContextRole role() {
        return OperationalContextRole.SUPER_ADMIN;
    }
}
