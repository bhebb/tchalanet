package com.tchalanet.server.catalog.settings.internal.persistence;

import com.tchalanet.server.catalog.settings.api.model.SettingExposure;
import com.tchalanet.server.catalog.settings.api.model.SettingLevel;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Setting Repository (INTERNAL)
 *
 * <p>This repository is INTERNAL and MUST NOT be exposed outside the catalog module.
 */
@Repository
public interface SettingRepository
    extends JpaRepository<SettingEntity, UUID>, JpaSpecificationExecutor<SettingEntity> {

  // ========================================
  // Resolution queries (read catalog)
  // ========================================

  // Global / generic by level (no tenant param) - already used for GLOBAL but also usable for other levels
  List<SettingEntity> findByActiveTrueAndDeletedAtIsNullAndLevel(SettingLevel level);

  // Level + namespace (no tenant param)
  List<SettingEntity> findByActiveTrueAndDeletedAtIsNullAndLevelAndNamespaceIn(
      SettingLevel level, Collection<String> namespaces);

  // Tenant-scoped queries (legacy/admin) - kept for admin flows
  List<SettingEntity> findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantId(
      SettingLevel level, UUID tenantId);

  List<SettingEntity> findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndNamespaceIn(
      SettingLevel level, UUID tenantId, Collection<String> namespaces);

  // ========================================
  // Admin queries (search/uniqueness)
  // ========================================

  Optional<SettingEntity>
      findFirstByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndNamespaceAndSettingKey(
          SettingLevel level,
          UUID tenantId,
          String namespace,
          String settingKey);

  Page<SettingEntity> findByActiveTrueAndDeletedAtIsNull(Pageable pageable);

  // Runtime exposure queries
  List<SettingEntity> findByActiveTrueAndDeletedAtIsNullAndExposure(SettingExposure exposure);

  List<SettingEntity> findByActiveTrueAndDeletedAtIsNullAndExposureAndNamespace(
      SettingExposure exposure, String namespace);
}
