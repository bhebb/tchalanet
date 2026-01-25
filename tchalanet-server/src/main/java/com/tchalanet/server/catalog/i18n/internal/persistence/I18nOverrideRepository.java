package com.tchalanet.server.catalog.i18n.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * I18n Override Repository (INTERNAL)
 *
 * <p>This repository is INTERNAL and MUST NOT be exposed outside the catalog module.
 */
@Repository
public interface I18nOverrideRepository
    extends JpaRepository<I18nOverrideEntity, UUID>,
        JpaSpecificationExecutor<I18nOverrideEntity> {

  // ========================================
  // Catalog queries (read catalog)
  // ========================================

  Optional<I18nOverrideEntity> findByIdAndDeletedAtIsNull(UUID id);

  List<I18nOverrideEntity> findByTenantIdAndLocaleAndActiveTrueAndDeletedAtIsNull(
      UUID tenantId, String locale);

  List<I18nOverrideEntity> findByTenantIdAndActiveTrueAndDeletedAtIsNull(UUID tenantId);

  // ========================================
  // Admin queries (pagination)
  // ========================================

  Page<I18nOverrideEntity> findByActiveTrueAndDeletedAtIsNull(Pageable pageable);

  Optional<I18nOverrideEntity>
      findFirstByTenantIdAndLocaleAndI18nKeyAndActiveTrueAndDeletedAtIsNull(
          UUID tenantId, String locale, String i18nKey);
}
