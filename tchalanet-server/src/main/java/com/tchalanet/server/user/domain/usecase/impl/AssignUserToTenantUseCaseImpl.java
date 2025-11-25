package com.tchalanet.server.user.domain.usecase.impl;

import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.tenant.domain.model.TenantUser;
import com.tchalanet.server.tenant.domain.ports.TenantUserRepository;
import com.tchalanet.server.user.domain.usecase.AssignUserToTenantUseCase;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class AssignUserToTenantUseCaseImpl implements AssignUserToTenantUseCase {

  private final TenantUserRepository tenantUserRepo;

  public AssignUserToTenantUseCaseImpl(TenantUserRepository tenantUserRepo) {
    this.tenantUserRepo = tenantUserRepo;
  }

  @Transactional
  @Override
  public TenantUser assign(UUID tenantId, UUID userId, String role) {
    // create domain object and save
    TenantUser t = new TenantUser(null, tenantId, userId, role, "none", false);
    return tenantUserRepo.save(t);
  }
}
