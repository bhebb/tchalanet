package com.tchalanet.server.features.tenantadmin.users.model;

import com.tchalanet.server.common.types.id.UserId;

public record TenantUserResponse(
    UserId id,
    UserId keycloakSub,
    String username,
    String email,
    String displayName
) {}
