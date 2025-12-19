package com.tchalanet.server.core.tenant.application.query.model;

import com.tchalanet.server.core.tenant.domain.model.TenantStatus;
import com.tchalanet.server.core.tenant.domain.model.TenantType;
import java.util.UUID;

public record TenantDto(
    UUID id,
    String code,
    String name,
    TenantType type,
    String timezone,
    String currency,
    TenantStatus status,
    UUID activeThemeId,
    UUID addressId
) {}
