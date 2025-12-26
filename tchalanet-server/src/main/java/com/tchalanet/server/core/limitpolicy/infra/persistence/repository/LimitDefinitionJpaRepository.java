package com.tchalanet.server.core.limitpolicy.infra.persistence.repository;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.core.limitpolicy.infra.persistence.entity.LimitDefinitionJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface LimitDefinitionJpaRepository extends JpaRepository<LimitDefinitionJpaEntity, UUID> {

  @Query("SELECT e FROM LimitDefinitionJpaEntity e WHERE e.tenantId = :tenantId AND e.enabled = true AND e.deletedAt IS NULL")
  List<LimitDefinitionJpaEntity> findActiveByTenantId(UUID tenantId);
}
