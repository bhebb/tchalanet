package com.tchalanet.server.core.accesscontrol.application.query.model;

import com.tchalanet.server.common.bus.Query;
import java.util.Set;
import java.util.UUID;

public record CheckUserPermissionsQuery(
    UUID tenantId,
    UUID userId,
    Set<String> requiredPermissions
) implements Query<Boolean> {}
