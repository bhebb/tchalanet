package com.tchalanet.server.tenantconfig.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record TenantSettingRequest(
    UUID id, // Null for creation, UUID for update
    @NotBlank String configKey,
    @NotBlank String configValue,
    @NotBlank String configType, // e.g., "STRING", "BOOLEAN", "INTEGER", "JSON"
    boolean active) {}
