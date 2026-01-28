package com.tchalanet.server.features.tenantadmin.infra.web.model;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.core.user.domain.model.UserPreference;

public record ProvisionTenantUserRequest(
    String email,
    String firstName,
    String lastName,
    RoleId roleId,
    UserPreference preferences) {
}
