package com.tchalanet.server.core.tenantconfig.domain.ports.in;

import com.tchalanet.server.core.tenantconfig.domain.model.TenantSetting;
import java.util.Optional;
import java.util.UUID;

/** Inbound Port for creating or updating tenant configuration settings. */
public interface UpsertTenantConfigUseCase {
  TenantSetting upsert(UpsertTenantConfigCommand command);

  Optional<TenantSetting> getTenantSetting(UUID settingId);

  record UpsertTenantConfigCommand(
      UUID id, // Null for creation, UUID for update
      UUID tenantId,
      String configKey,
      String configValue,
      String configType, // e.g., "STRING", "BOOLEAN", "INTEGER", "JSON"
      boolean active) {}
}
