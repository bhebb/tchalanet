package com.tchalanet.server.catalog.settings.internal.read;

import com.tchalanet.server.catalog.settings.api.model.ResolveSettingsCriteria;
import com.tchalanet.server.catalog.settings.api.model.ResolvedSettingView;
import com.tchalanet.server.catalog.settings.api.model.SettingLevel;
import com.tchalanet.server.catalog.settings.api.SettingsCatalog;
import com.tchalanet.server.catalog.settings.api.model.SettingsCatalogStatsView;
import com.tchalanet.server.catalog.settings.internal.cache.SettingsCacheNames;
import com.tchalanet.server.catalog.settings.internal.mapper.SettingMapper;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingEntity;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settings Catalog Implementation (READ SIDE)
 *
 * <p>Implements the SettingsCatalog contract with hierarchical resolution and caching.
 *
 * <p>This is the ONLY implementation of read operations. It MUST NOT perform writes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SettingsCatalogImpl implements SettingsCatalog {

  private final SettingRepository repository;
  private final SettingMapper mapper;

  @Override
  @Cacheable(
      value = SettingsCacheNames.RESOLVED_SETTINGS,
      key = "T(com.tchalanet.server.catalog.settings.internal.cache.SettingsCacheKey).of(#criteria)")
  public List<ResolvedSettingView> resolve(ResolveSettingsCriteria criteria) {
    List<String> namespaces = criteria.namespaces();

    log.debug(
        "Resolving settings for tenant={}, namespaces={}",
        criteria.tenantId(),
        namespaces);

    // Handle empty namespace filter (= all namespaces)
    List<String> effectiveNamespaces =
        (namespaces == null || namespaces.isEmpty()) ? List.of() : namespaces;

    // Map to store merged settings (namespace.key → resolved view)
    Map<String, ResolvedSettingView> merged = new LinkedHashMap<>();

    // 1. GLOBAL level (platform defaults)
    mergeLevel(merged, loadGlobal(effectiveNamespaces), SettingLevel.GLOBAL);

    // 2. TENANT level (tenant overrides)
    mergeLevel(merged, loadForTenant(effectiveNamespaces), SettingLevel.TENANT);

    List<ResolvedSettingView> result = List.copyOf(merged.values());
    log.debug("Resolved {} settings", result.size());
    return result;
  }

  // ========================================
  // Load methods (persistence access)
  // ========================================

  private List<SettingEntity> loadGlobal(List<String> namespaces) {
    if (namespaces.isEmpty()) {
      // Special handling: load ALL active global settings
      return repository.findByActiveTrueAndDeletedAtIsNullAndLevel(SettingLevel.GLOBAL);
    }
    return repository.findByActiveTrueAndDeletedAtIsNullAndLevelAndNamespaceIn(
        SettingLevel.GLOBAL, namespaces);
  }

  private List<SettingEntity> loadForTenant(List<String> namespaces) {
    // RLS will scope by current tenant; do NOT pass tenantId explicitly
    if (namespaces.isEmpty()) {
      return repository.findByActiveTrueAndDeletedAtIsNullAndLevel(SettingLevel.TENANT);
    }
    return repository.findByActiveTrueAndDeletedAtIsNullAndLevelAndNamespaceIn(
        SettingLevel.TENANT, namespaces);
  }

  // ========================================
  // Merge logic
  // ========================================

  private void mergeLevel(
      Map<String, ResolvedSettingView> merged,
      List<SettingEntity> entities,
      SettingLevel effectiveLevel) {

    for (SettingEntity entity : entities) {
      String key = makeKey(entity.getNamespace(), entity.getSettingKey());
      // Later levels overwrite earlier levels (more specific wins)
      merged.put(key, mapper.toResolvedView(entity, effectiveLevel));
    }
  }

  private static String makeKey(String namespace, String key) {
    return namespace + "\u0000" + key;
  }

  @Override
  public SettingsCatalogStatsView stats() {
    // total global settings (GLOBAL level)
    int totalGlobal = repository.findByActiveTrueAndDeletedAtIsNullAndLevel(SettingLevel.GLOBAL).size();
    // total tenant settings (TENANT level)
    int totalTenant = repository.findByActiveTrueAndDeletedAtIsNullAndLevel(SettingLevel.TENANT).size();
    // total active settings across all levels
    long totalActiveLong = repository.findByActiveTrueAndDeletedAtIsNull(Pageable.unpaged()).getTotalElements();
    int totalActive = (int) totalActiveLong; // Cast to int
    return new SettingsCatalogStatsView(totalGlobal, totalTenant, totalActive);
  }
}
