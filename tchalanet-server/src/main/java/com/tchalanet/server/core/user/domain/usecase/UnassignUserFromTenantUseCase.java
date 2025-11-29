package com.tchalanet.server.core.user.domain.usecase;

import java.util.UUID;

public interface UnassignUserFromTenantUseCase {
  void unassign(UUID tenantId, UUID userId);
}
