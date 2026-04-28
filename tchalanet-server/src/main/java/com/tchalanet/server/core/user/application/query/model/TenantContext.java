package com.tchalanet.server.core.user.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;
import jakarta.annotation.Nullable;

public record TenantContext(
    @Nullable TenantId tenantId,
    String tenantCode,
    String timeZone,
    String currency) {}
