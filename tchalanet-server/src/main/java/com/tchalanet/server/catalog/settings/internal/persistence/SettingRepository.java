package com.tchalanet.server.catalog.settings.internal.persistence;

import com.tchalanet.server.catalog.settings.api.SettingLevel;
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

  // Global (no namespace filter)
  List<SettingEntity> findByActiveTrueAndDeletedAtIsNullAndLevel(SettingLevel level);

  // Global (with namespace filter)
  List<SettingEntity> findByActiveTrueAndDeletedAtIsNullAndLevelAndNamespaceIn(
      SettingLevel level, Collection<String> namespaces);

  // Tenant (no namespace filter)
  List<SettingEntity> findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantId(
      SettingLevel level, UUID tenantId);

  // Tenant (with namespace filter)
  List<SettingEntity> findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndNamespaceIn(
      SettingLevel level, UUID tenantId, Collection<String> namespaces);

  // Outlet (no namespace filter)
  List<SettingEntity> findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletId(
      SettingLevel level, UUID tenantId, UUID outletId);

  // Outlet (with namespace filter)
  List<SettingEntity>
      findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletIdAndNamespaceIn(
          SettingLevel level, UUID tenantId, UUID outletId, Collection<String> namespaces);

  // Terminal (no namespace filter)
  List<SettingEntity> findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndTerminalId(
      SettingLevel level, UUID tenantId, UUID terminalId);

  // Terminal (with namespace filter)
  List<SettingEntity>
      findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndTerminalIdAndNamespaceIn(
          SettingLevel level, UUID tenantId, UUID terminalId, Collection<String> namespaces);

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
