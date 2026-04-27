package com.tchalanet.server.catalog.settings.internal.persistence;

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

  // Outlet variants WITHOUT tenantId (new): RLS will scope to current tenant;
  // these are intended for the read-side (SettingsCatalogImpl) to call without passing tenantId.
  List<SettingEntity> findByActiveTrueAndDeletedAtIsNullAndLevelAndOutletId(
      SettingLevel level, UUID outletId);

  List<SettingEntity>
      findByActiveTrueAndDeletedAtIsNullAndLevelAndOutletIdAndNamespaceIn(
          SettingLevel level, UUID outletId, Collection<String> namespaces);

  // Terminal variants WITHOUT tenantId (new)
  List<SettingEntity> findByActiveTrueAndDeletedAtIsNullAndLevelAndTerminalId(
      SettingLevel level, UUID terminalId);

  List<SettingEntity>
      findByActiveTrueAndDeletedAtIsNullAndLevelAndTerminalIdAndNamespaceIn(
          SettingLevel level, UUID terminalId, Collection<String> namespaces);

  // ========================================
  // Admin queries (search/uniqueness)
  // ========================================

  Optional<SettingEntity>
      findFirstByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletIdAndTerminalIdAndNamespaceAndSettingKey(
          SettingLevel level,
          UUID tenantId,
          UUID outletId,
          UUID terminalId,
          String namespace,
          String settingKey);

  Page<SettingEntity> findByActiveTrueAndDeletedAtIsNull(Pageable pageable);
}
