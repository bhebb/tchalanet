package com.tchalanet.server.core.user.domain.usecase;

import com.tchalanet.server.core.tenant.domain.model.TenantUser;
import java.util.UUID;

public interface AssignUserToTenantUseCase {
  TenantUser assign(UUID tenantId, UUID userId, String role);
}
