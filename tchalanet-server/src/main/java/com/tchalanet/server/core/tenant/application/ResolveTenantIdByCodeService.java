package com.tchalanet.server.core.tenant.application;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.core.tenant.application.ports.in.ResolveTenantIdByCodeUseCase;
import com.tchalanet.server.core.tenant.infra.persistence.TenantJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service; // Changed from @Component to @Service for

// application layer

@Service // Changed from @Component to @Service for application layer
@RequiredArgsConstructor
public class ResolveTenantIdByCodeService implements ResolveTenantIdByCodeUseCase {

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
