package com.tchalanet.server.features.pagemodel.shared;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "page-models")
public interface PageModelRepository extends JpaRepository<PageModelEntity, UUID> {

    List<PageModelEntity> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    Optional<PageModelEntity> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<PageModelEntity> findByTenantIdAndScopeAndSlugAndDeletedAtIsNull(UUID tenantId, String scope, String slug);

    Optional<PageModelEntity> findByTenantIdAndLogicalIdAndDeletedAtIsNull(UUID tenantId, String logicalId);

    Optional<PageModelEntity> findByTenantIdAndLogicalIdAndStatusAndDeletedAtIsNull(
        UUID tenantId,
        String logicalId,
        PageStatus status
    );

    List<PageModelEntity> findAllByTenantIdAndLogicalId(UUID tenantId, String logicalId);

    // Recherches basées sur template_id
    List<PageModelEntity> findAllByTemplateIdAndDeletedAtIsNull(UUID templateId);

    Optional<PageModelEntity> findByTenantIdAndTemplateIdAndDeletedAtIsNull(UUID tenantId, UUID templateId);
}
