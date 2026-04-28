package com.tchalanet.server.core.user.infra.web.model;

import com.tchalanet.server.common.types.id.TenantId;
import jakarta.annotation.Nullable;

public record TenantContextResponse(
    @Nullable TenantId tenantId,
    String tenantCode,
    String timeZone,
    String currency) {}
