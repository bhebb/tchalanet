package com.tchalanet.server.core.tenantuser.infra.web.admin.model;

import com.tchalanet.server.common.types.id.UserId;

public record TenantUserAdminResponse(
    UserId id,
    String keycloakSub,
    String username,
    String email,
    String firstName,
    String lastName,
    String displayName
) {}
