package com.tchalanet.server.tenant.infra.persistence;

import com.tchalanet.server.common.config.SpringContextHolder;
import com.tchalanet.server.common.usecase.ResolveTenantUseCase;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;

public class TenantCacheEvictListener {

  @PostPersist
  @PostUpdate
  public void onChange(TenantJpaEntity entity) {
    if (entity == null) return;
    try {
      ResolveTenantUseCase resolver = SpringContextHolder.getBean(ResolveTenantUseCase.class);
      if (resolver != null && entity.getCode() != null) {
        resolver.evictByCode(entity.getCode());
      }
    } catch (Exception ex) {
      // avoid failing persistence operation
    }
  }
}
