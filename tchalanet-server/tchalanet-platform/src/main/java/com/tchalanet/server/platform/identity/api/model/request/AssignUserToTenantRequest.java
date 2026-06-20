package com.tchalanet.server.platform.identity.api.model.request;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record AssignUserToTenantRequest(
    TenantId tenantId,
    UserId userId,
    RoleId roleId,
    boolean owner) {}
