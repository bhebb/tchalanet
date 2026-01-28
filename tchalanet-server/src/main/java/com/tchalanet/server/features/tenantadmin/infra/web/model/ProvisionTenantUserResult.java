package com.tchalanet.server.features.tenantadmin.infra.web.model;

import com.tchalanet.server.common.types.id.UserId;

public record ProvisionTenantUserResult(UserId userId, boolean isNew) {}
