package com.tchalanet.server.features.tenantadmin.application.command.model;

import com.tchalanet.server.common.types.id.UserId;

public record ProvisionTenantUserResult(UserId userId, boolean isNew) {}
