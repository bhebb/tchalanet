package com.tchalanet.server.features.tenantadmin.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.user.domain.model.UserPreference;

public record ProvisionTenantUserCommand(
    TenantId tenantId,
    String email,
    String firstName,
    String lastName,
    com.tchalanet.server.common.types.id.RoleId roleId,
    UserPreference preferences)
    implements Command<com.tchalanet.server.features.tenantadmin.application.command.model.ProvisionTenantUserResult> {}
