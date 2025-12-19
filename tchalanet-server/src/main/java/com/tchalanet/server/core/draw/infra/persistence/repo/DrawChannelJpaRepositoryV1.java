package com.tchalanet.server.core.draw.infra.persistence.repo;

import com.tchalanet.server.core.draw.infra.persistence.DrawChannelJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrawChannelJpaRepositoryV1 extends JpaRepository<DrawChannelJpaEntity, UUID> {
  List<DrawChannelJpaEntity> findByTenantIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc(
      UUID tenantId);
}

