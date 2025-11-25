package com.tchalanet.server.user.domain.usecase.impl;

import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.tenant.domain.ports.TenantUserRepository;
import com.tchalanet.server.user.domain.usecase.UnassignUserFromTenantUseCase;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class UnassignUserFromTenantUseCaseImpl implements UnassignUserFromTenantUseCase {

  private final TenantUserRepository tenantUserRepository;

  public UnassignUserFromTenantUseCaseImpl(TenantUserRepository tenantUserRepository) {
    this.tenantUserRepository = tenantUserRepository;
  }

  @Transactional
  @Override
  public void unassign(UUID tenantId, UUID userId) {
    tenantUserRepository.deleteByTenantIdAndUserId(tenantId, userId);
  }
}
