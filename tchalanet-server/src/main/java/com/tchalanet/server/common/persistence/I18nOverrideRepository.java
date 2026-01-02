package com.tchalanet.server.common.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(exported = true, path = "i18n-overrides")
public interface I18nOverrideRepository extends JpaRepository<I18nOverrideEntity, UUID> {

  @RestResource(path = "all-by-tenant", rel = "all-by-tenant")
  List<I18nOverrideEntity> findByTenantId(UUID tenantId);

  Page<I18nOverrideEntity> findByTenantIdAndLocaleIgnoreCase(
      UUID tenantId, String locale, Pageable pageable);

  @RestResource(path = "by-tenant", rel = "by-tenant")
  Page<I18nOverrideEntity> findByTenantId(UUID tenantId, Pageable pageable);
}
