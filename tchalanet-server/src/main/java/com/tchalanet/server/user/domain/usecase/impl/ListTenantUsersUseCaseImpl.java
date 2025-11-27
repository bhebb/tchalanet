package com.tchalanet.server.user.domain.usecase.impl;

import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.user.domain.model.AppUser;
import com.tchalanet.server.user.domain.ports.AppUserRepository;
import com.tchalanet.server.user.domain.usecase.ListTenantUsersUseCase;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListTenantUsersUseCaseImpl implements ListTenantUsersUseCase {

  private final AppUserRepository repo;

  @Override
  public List<AppUser> listByTenant(UUID tenantId) {
    return repo.findByTenantId(tenantId);
  }
}
