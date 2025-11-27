package com.tchalanet.server.tenantconfig.web.dto;

import java.time.Instant;
import java.util.UUID;

public record TenantSettingResponse(
    UUID id,
    UUID tenantId,
    String configKey,
    String configValue,
    String configType,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {}
