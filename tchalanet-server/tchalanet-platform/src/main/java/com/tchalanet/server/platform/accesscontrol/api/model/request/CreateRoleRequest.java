package com.tchalanet.server.platform.accesscontrol.api.model.request;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;

public record CreateRoleRequest(
    RoleId id,
    String code,
    String name,
    String description,
    TenantId tenantId,
    RoleId parentRoleId,
    boolean system){}


