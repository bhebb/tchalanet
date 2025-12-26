package com.tchalanet.server.core.accesscontrol.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.Set;

public record CheckUserPermissionsQuery(
    TenantId tenantId,
    UserId userId,
    Set<String> requiredPermissions
) implements Query<Boolean> {}
