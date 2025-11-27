package com.tchalanet.server.common.usecase;

import java.util.Optional;
import java.util.UUID;

public interface ResolveTenantUseCase {
  Optional<UUID> resolveIdByCode(String tenantCode);
}
