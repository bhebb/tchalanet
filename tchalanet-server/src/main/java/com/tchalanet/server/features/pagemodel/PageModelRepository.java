package com.tchalanet.server.features.pagemodel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RepositoryRestResource(exported = true, path = "page-models")
public interface PageModelRepository extends JpaRepository<PageModelEntity, UUID> {

    // List du tenant courant (RLS)
    List<PageModelEntity> findByDeletedAtIsNull();

    Optional<PageModelEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<PageModelEntity> findByScopeAndSlugAndDeletedAtIsNull(String scope, String slug);

    Optional<PageModelEntity> findByLogicalIdAndDeletedAtIsNull(String logicalId);

    Optional<PageModelEntity> findByLogicalIdAndStatusAndDeletedAtIsNull(
        String logicalId, PageStatus status);

    Optional<PageModelEntity> findByTenantIdAndLogicalIdAndStatusAndDeletedAtIsNull(
        UUID tenantId, String logicalId, PageStatus status);

    List<PageModelEntity> findAllByLogicalId(String logicalId);

    // template_id: ATTENTION -> si template partagé cross-tenant, voir note plus bas
    List<PageModelEntity> findAllByTemplateIdAndDeletedAtIsNull(UUID templateId);

    boolean existsByLogicalIdAndDeletedAtIsNull(String logicalId);
}
