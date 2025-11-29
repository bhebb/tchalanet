package com.tchalanet.server.core.tenant.infra.persistence;

import com.tchalanet.server.common.config.SpringContextHolder;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;

public class TenantCacheEvictListener {

  @PostPersist
  @PostUpdate
  public void onChange(TenantJpaEntity entity) {
    if (entity == null) return;
    try {
      var resolver =
          SpringContextHolder.getBean(
              com.tchalanet.server.core.tenant.application.ports.in.ResolveTenantIdByCodeUseCase
                  .class);
      if (resolver != null && entity.getCode() != null) {
        resolver.evictByCode(entity.getCode());
      }
    } catch (Exception ignored) {
      // avoid failing persistence operation
    }
  }
}
