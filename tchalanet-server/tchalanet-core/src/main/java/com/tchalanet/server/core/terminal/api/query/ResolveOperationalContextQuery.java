package com.tchalanet.server.core.terminal.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record ResolveOperationalContextQuery(
    TenantId tenantId,
    UserId actorUserId,
    OperationalContextHint candidate,
    String bindingCredential
) implements Query<OperationalContextHint> {}
