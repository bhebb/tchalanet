package com.tchalanet.server.common.persistence;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "admin/i18n-overrides", collectionResourceRel = "i18n-overrides")
public interface I18nOverrideRepository extends JpaRepository<I18nOverrideEntity, UUID> {
    // Page d’overrides pour un tenant
    Page<I18nOverrideEntity> findByTenantId(UUID tenantId, Pageable pageable);

    // Page d’overrides pour un tenant + locale donnée
    Page<I18nOverrideEntity> findByTenantIdAndLocaleIgnoreCase(
        UUID tenantId, String locale, Pageable pageable);
}
