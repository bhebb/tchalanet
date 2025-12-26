package com.tchalanet.server.core.limitpolicy.application.query.model;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.enums.TargetType;
import java.util.UUID;

public record GetLimitAssignmentsQuery(TenantId tenantId, TargetType targetType, UUID targetId) implements Query<GetLimitAssignmentsResult> {}
