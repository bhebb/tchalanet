package com.tchalanet.server.core.tenantconfig.domain.ports.in;

import java.util.Optional;
import java.util.UUID;

/** Inbound Port for retrieving tenant-specific configuration settings. */
public interface GetTenantConfigUseCase {
  Optional<String> getString(UUID tenantId, String configKey);

  Optional<Boolean> getBoolean(UUID tenantId, String configKey);

  Optional<Integer> getInteger(UUID tenantId, String configKey);
  // Add other types as needed (e.g., BigDecimal, JSON)
}
