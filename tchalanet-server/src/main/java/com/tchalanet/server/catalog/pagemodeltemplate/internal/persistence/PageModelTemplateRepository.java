package com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageModelTemplateRepository extends JpaRepository<PageModelTemplateEntity, UUID> {

  List<PageModelTemplateEntity> findAllByLogicalIdAndDeletedAtIsNull(String logicalId);

  Optional<PageModelTemplateEntity>
      findByLogicalIdAndIsDefaultTrueAndIsSystemTrueAndDeletedAtIsNull(String logicalId);

  List<PageModelTemplateEntity> findAllByIsSystemTrueAndDeletedAtIsNull();

  List<PageModelTemplateEntity> findAllByTenantIdAndDeletedAtIsNull(UUID tenantId);
}
