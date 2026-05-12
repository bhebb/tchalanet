package com.tchalanet.server.platform.accesscontrol.api.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.UUID;

public record CreateRoleRequest(
    RoleId id,
    String code,
    String name,
    String description,
    TenantId tenantId,
    RoleId parentRoleId,
    boolean system)
    implements Command<UUID> {}

