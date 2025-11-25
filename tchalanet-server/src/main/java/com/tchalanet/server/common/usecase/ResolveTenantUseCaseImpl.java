package com.tchalanet.server.common.usecase;

import com.tchalanet.server.common.infra.persistence.BaseEntity;
import com.tchalanet.server.tenant.infra.persistence.TenantJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResolveTenantUseCaseImpl implements ResolveTenantUseCase {

  private final TenantJpaRepository tenantRepo;

  @Override
  @Cacheable(value = "tenantCodeToId", unless = "#result==null || #result.isEmpty()")
  public Optional<UUID> resolveIdByCode(String tenantCode) {
    if (tenantCode == null || tenantCode.isBlank()) return Optional.empty();
    return tenantRepo.findByCode(tenantCode).map(BaseEntity::getId);
  }

  @Override
  @CacheEvict(value = "tenantCodeToId", key = "#tenantCode")
  public void evictByCode(String tenantCode) {
    // cache eviction handled by annotation
  }

  @Override
  @CacheEvict(value = "tenantCodeToId", allEntries = true)
  public void evictAll() {
    // cache eviction handled by annotation
  }
}
