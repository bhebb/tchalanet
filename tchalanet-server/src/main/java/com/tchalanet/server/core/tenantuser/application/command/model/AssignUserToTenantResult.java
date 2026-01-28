package com.tchalanet.server.core.tenantuser.application.command.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record AssignUserToTenantResult(TenantId tenantId, UserId userId, boolean isNew) {}
