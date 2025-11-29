package com.tchalanet.server.core.user.domain.usecase;

import com.tchalanet.server.core.user.domain.model.AppUser;
import java.util.List;
import java.util.UUID;

public interface ListTenantUsersUseCase {
  List<AppUser> listByTenant(UUID tenantId);
}
