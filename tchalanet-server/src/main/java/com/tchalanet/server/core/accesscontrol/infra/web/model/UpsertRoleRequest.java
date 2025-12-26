package com.tchalanet.server.core.accesscontrol.infra.web.dto;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.UUID;

public record UpsertRoleRequest(
    UUID id,
    String code,
    String name,
    String description,
    UUID tenantId,
    UUID parentRoleId,
    boolean system) {}
