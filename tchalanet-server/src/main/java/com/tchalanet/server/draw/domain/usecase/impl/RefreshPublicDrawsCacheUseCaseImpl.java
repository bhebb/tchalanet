package com.tchalanet.server.draw.domain.usecase.impl;

import com.tchalanet.server.common.domain.UseCase;
import com.tchalanet.server.draw.domain.usecase.GetPublicDrawSummaryUseCase;
import com.tchalanet.server.draw.domain.usecase.RefreshPublicDrawsCacheUseCase;
import com.tchalanet.server.tenant.infra.persistence.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RefreshPublicDrawsCacheUseCaseImpl implements RefreshPublicDrawsCacheUseCase {

  private final TenantJpaRepository tenantRepo;
  private final GetPublicDrawSummaryUseCase summaryUseCase;

  @Override
  public void refreshAllTenants() {
    tenantRepo
        .findAll()
        .forEach(
            t -> {
              try {
                summaryUseCase.getSummaryForTenant(t.getId());
                log.debug("Warmed cache for tenant {}", t.getCode());
              } catch (Exception e) {
                log.warn("Failed to refresh cache for tenant {}: {}", t.getCode(), e.getMessage());
              }
            });
  }
}
