package com.tchalanet.server.core.tenant.infra.cache;

import com.tchalanet.server.common.config.SpringContextHolder;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.tenant.infra.persistence.TenantJpaEntity;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantCacheEvictListener {

  @PostPersist
  @PostUpdate
  public void onChange(TenantJpaEntity entity) {
    if (entity == null) return;
    try {
      var cache = SpringContextHolder.getBean(TenantCache.class);
      if (cache == null) return;

      var codeLower = entity.getCode() == null ? null : entity.getCode().trim().toLowerCase();
      cache.evictAfterCommit(TenantId.of(entity.getId()), codeLower);

    } catch (Exception ex) {
      log.error("Tenant cache evict failed: {}", ex.getMessage(), ex);
    }
  }
}
