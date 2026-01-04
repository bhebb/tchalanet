package com.tchalanet.server.core.tenant.application.query.model;

import com.tchalanet.server.common.types.enums.TenantStatus;
import com.tchalanet.server.common.types.enums.TenantType;
import com.tchalanet.server.core.address.application.dto.AddressDto;
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
    UUID addressId,
    AddressDto address) {}
