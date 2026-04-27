package com.tchalanet.server.core.pagemodel.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageModelJpaRepository extends JpaRepository<PageModelJpaEntity, UUID> {
  Optional<PageModelJpaEntity> findFirstByTenantIdAndLogicalIdAndStatusAndDeletedAtIsNull(UUID tenantId, String logicalId, String status);
  Optional<PageModelJpaEntity> findFirstByLogicalIdAndStatusAndDeletedAtIsNull(String logicalId, String status);
  List<PageModelJpaEntity> findAllByLogicalIdAndStatusAndDeletedAtIsNull(String logicalId, String status);
  List<PageModelJpaEntity> findAllByDeletedAtIsNull();
  // [Phase 3B] version paginée pour ListPageModelsHandler
  Page<PageModelJpaEntity> findAllByDeletedAtIsNull(Pageable pageable);
  // [Phase 4C] recherche par templateId pour propagation template → instances
  List<PageModelJpaEntity> findAllByTemplateIdAndDeletedAtIsNull(UUID templateId);
}
