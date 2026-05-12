package com.tchalanet.server.platform.accesscontrol.api.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.Set;

public record CheckUserPermissionsRequest(
    TenantId tenantId, UserId userId, Set<String> requiredPermissions)
    implements Query<CheckUserPermissionsResult> {}

