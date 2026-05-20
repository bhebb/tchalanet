package com.tchalanet.server.core.session.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;

public record ResolvePosOperationContextQuery(
    TenantId tenantId,
    UserId actorUserId,
    OperationalContextHint operationalContext,
    PosOperationAction action
) implements Query<ValidatedPosOperationContext> {}
