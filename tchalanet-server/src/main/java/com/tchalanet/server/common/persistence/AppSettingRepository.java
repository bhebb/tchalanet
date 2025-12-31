package com.tchalanet.server.common.persistence;

import com.tchalanet.server.common.settings.AppSettingLevel;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

// Platform settings should not be auto-exposed by Spring Data REST; controllers will serve
// /platform/*
@RepositoryRestResource(
    exported = false,
    path = "app-settings",
    collectionResourceRel = "app-settings")
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

  @RestResource(path = "global", rel = "global")
  default List<AppSettingEntity> findGlobalByNamespace(Collection<String> namespaces) {
    return findByActiveTrueAndDeletedAtIsNullAndLevelAndNamespaceIn(
        AppSettingLevel.GLOBAL, namespaces);
  }
}
