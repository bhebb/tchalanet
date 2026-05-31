package com.tchalanet.server.catalog.i18n.internal.persistence;

import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideLevel;
import com.tchalanet.server.catalog.i18n.api.model.I18nSurface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * I18n Override Repository (INTERNAL)
 *
 * <p>This repository is INTERNAL and MUST NOT be exposed outside the catalog module.
 */
@Repository
public interface I18nOverrideRepository
    extends JpaRepository<I18nOverrideEntity, UUID>,
    JpaSpecificationExecutor<I18nOverrideEntity> {


    List<I18nOverrideEntity> findByLocaleAndLevelAndActiveTrueAndDeletedAtIsNull(
        String locale, I18nOverrideLevel level);
    // ========================================
    // Catalog queries (read catalog)
    // ========================================

    Optional<I18nOverrideEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Tenant-aware first-by-key (admin/write flows)
    Optional<I18nOverrideEntity>
    findFirstByTenantIdAndLocaleAndI18nKeyAndActiveTrue(
        UUID tenantId, String locale, String i18nKey);


    // ========================================
    // Admin queries (pagination)
    // ========================================

    Page<I18nOverrideEntity> findByActiveTrue(Pageable pageable);

    Optional<I18nOverrideEntity> findFirstByLocaleAndI18nKeyAndLevel(String loc, String key, I18nOverrideLevel i18nOverrideLevel);

    List<I18nOverrideEntity> findByLocaleAndLevelAndTenantIdAndActiveTrueAndDeletedAtIsNull(String loc, I18nOverrideLevel i18nOverrideLevel, UUID tenantId);

    // NEW: list all active global overrides (for stats)
    List<I18nOverrideEntity> findByLevelAndActiveTrueAndDeletedAtIsNull(I18nOverrideLevel level);

    // Runtime bundle queries: surface IN (:surfaces)
    List<I18nOverrideEntity> findByLocaleAndLevelAndSurfaceInAndActiveTrueAndDeletedAtIsNull(
        String locale, I18nOverrideLevel level, java.util.Collection<I18nSurface> surfaces);

    List<I18nOverrideEntity> findByLocaleAndLevelAndTenantIdAndSurfaceInAndActiveTrueAndDeletedAtIsNull(
        String locale, I18nOverrideLevel level, UUID tenantId, java.util.Collection<I18nSurface> surfaces);
}
