package com.tchalanet.server.user.domain.usecase;

import com.tchalanet.server.user.domain.model.AppUser;
import java.util.List;
import java.util.UUID;

public interface ListTenantUsersUseCase {
  List<AppUser> listByTenant(UUID tenantId);
}
