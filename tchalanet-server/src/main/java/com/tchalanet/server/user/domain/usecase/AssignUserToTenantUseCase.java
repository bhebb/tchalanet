package com.tchalanet.server.user.domain.usecase;

import com.tchalanet.server.tenant.domain.model.TenantUser;
import java.util.UUID;

public interface AssignUserToTenantUseCase {
  TenantUser assign(UUID tenantId, UUID userId, String role);
}
