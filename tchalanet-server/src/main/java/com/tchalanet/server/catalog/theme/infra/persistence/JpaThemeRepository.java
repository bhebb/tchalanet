package com.tchalanet.server.catalog.theme.infra.persistence;

import com.tchalanet.server.catalog.theme.domain.model.ThemeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface JpaThemeRepository extends JpaRepository<ThemeJpaEntity, UUID> {

  Optional<ThemeJpaEntity> findById(UUID id);

  // Return all themes for a tenant (non-paginated) - exposed as /all-by-tenant
  List<ThemeJpaEntity> findByTenantId(UUID tenantId);

  // Return paged themes for a tenant - exposed as /by-tenant
  Page<ThemeJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

  List<ThemeJpaEntity> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, ThemeStatus status);

  Page<ThemeJpaEntity> findByStatusAndDeletedAtIsNull(ThemeStatus status, Pageable pageable);
}
