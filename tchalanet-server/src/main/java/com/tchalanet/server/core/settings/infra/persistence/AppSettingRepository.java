package com.tchalanet.server.core.settings.infra.persistence;

import com.tchalanet.server.core.settings.AppSettingLevel;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSettingEntity, UUID> {
  List<AppSettingEntity> findByActiveTrueAndDeletedAtIsNullAndLevelAndNamespaceIn(
      AppSettingLevel level, Collection<String> namespaces);

  List<AppSettingEntity> findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndNamespaceIn(
      AppSettingLevel level, UUID tenantId, Collection<String> namespaces);

  List<AppSettingEntity>
      findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletIdAndNamespaceIn(
          AppSettingLevel level, UUID tenantId, UUID outletId, Collection<String> namespaces);

  List<AppSettingEntity>
      findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndTerminalIdAndNamespaceIn(
          AppSettingLevel level, UUID tenantId, UUID terminalId, Collection<String> namespaces);

  Optional<AppSettingEntity>
      findFirstByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletIdAndTerminalIdAndNamespaceAndSettingKey(
          AppSettingLevel level,
          UUID tenantId,
          UUID outletId,
          UUID terminalId,
          String namespace,
          String settingKey);

  default List<AppSettingEntity> findGlobalByNamespace(Collection<String> namespaces) {
    return findByActiveTrueAndDeletedAtIsNullAndLevelAndNamespaceIn(
        AppSettingLevel.GLOBAL, namespaces);
  }
}
