package com.tchalanet.server.core.accesscontrol.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record GetEffectivePermissionsQuery(UserId userId, TenantId tenantId) {}
