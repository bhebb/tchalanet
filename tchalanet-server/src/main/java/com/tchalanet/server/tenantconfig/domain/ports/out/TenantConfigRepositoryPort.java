package com.tchalanet.server.tenantconfig.domain.ports.out;

import com.tchalanet.server.tenantconfig.domain.model.TenantSetting;
import java.util.Optional;
import java.util.UUID;

/** Outbound Port for persisting and retrieving TenantSetting aggregates. */
public interface TenantConfigRepositoryPort {
  TenantSetting save(TenantSetting setting);

  Optional<TenantSetting> findById(UUID settingId);

  Optional<TenantSetting> findByTenantIdAndConfigKey(UUID tenantId, String configKey);
}
