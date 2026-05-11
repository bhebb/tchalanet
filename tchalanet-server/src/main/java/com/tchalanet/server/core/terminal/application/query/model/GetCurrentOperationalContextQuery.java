package com.tchalanet.server.core.terminal.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record GetCurrentOperationalContextQuery(
    TenantId tenantId,
    UserId userId,
    String terminalIdHeader,
    String deviceId,
    String terminalBinding
) implements Query<CurrentOperationalContextView> {
}
