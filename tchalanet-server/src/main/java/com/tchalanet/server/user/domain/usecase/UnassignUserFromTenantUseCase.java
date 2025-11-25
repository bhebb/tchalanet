package com.tchalanet.server.user.domain.usecase;

import java.util.UUID;

public interface UnassignUserFromTenantUseCase {
  void unassign(UUID tenantId, UUID userId);
}
