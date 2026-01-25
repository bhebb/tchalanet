package com.tchalanet.server.catalog.settings.internal.read;

import com.tchalanet.server.catalog.settings.api.ResolvedSettingView;
import com.tchalanet.server.catalog.settings.api.SettingLevel;
import com.tchalanet.server.catalog.settings.api.SettingsCatalog;
import com.tchalanet.server.catalog.settings.internal.cache.SettingsCacheNames;
import com.tchalanet.server.catalog.settings.internal.mapper.SettingMapper;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingEntity;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingRepository;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
      key = "T(com.tchalanet.server.catalog.settings.internal.cache.SettingsCacheKey).of(#tenantId, #outletId, #terminalId, #namespaces)")
  public List<ResolvedSettingView> resolve(
      TenantId tenantId, OutletId outletId, TerminalId terminalId, List<String> namespaces) {

    log.debug(
        "Resolving settings for tenant={}, outlet={}, terminal={}, namespaces={}",
        tenantId,
        outletId,
        terminalId,
        namespaces);

    // Handle empty namespace filter (= all namespaces)
    List<String> effectiveNamespaces = (namespaces == null || namespaces.isEmpty())
        ? List.of()
        : namespaces;

    // Map to store merged settings (namespace.key → resolved view)
    Map<String, ResolvedSettingView> merged = new LinkedHashMap<>();

    // 1. GLOBAL level (platform defaults)
    mergeLevel(merged, loadGlobal(effectiveNamespaces), SettingLevel.GLOBAL);

    // 2. TENANT level (tenant overrides)
    mergeLevel(merged, loadForTenant(tenantId, effectiveNamespaces), SettingLevel.TENANT);

    // 3. OUTLET level (outlet overrides, if outlet provided)
    if (outletId != null) {
      mergeLevel(
          merged, loadForOutlet(tenantId, outletId, effectiveNamespaces), SettingLevel.OUTLET);
    }

    // 4. TERMINAL level (terminal overrides, if terminal provided)
    if (terminalId != null) {
      mergeLevel(
          merged,
          loadForTerminal(tenantId, terminalId, effectiveNamespaces),
          SettingLevel.TERMINAL);
    }

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

  private List<SettingEntity> loadForTenant(TenantId tenantId, List<String> namespaces) {
    if (namespaces.isEmpty()) {
      return repository.findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantId(
          SettingLevel.TENANT, tenantId.value());
    }
    return repository.findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndNamespaceIn(
        SettingLevel.TENANT, tenantId.value(), namespaces);
  }

  private List<SettingEntity> loadForOutlet(
      TenantId tenantId, OutletId outletId, List<String> namespaces) {
    if (namespaces.isEmpty()) {
      return repository.findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletId(
          SettingLevel.OUTLET, tenantId.value(), outletId.value());
    }
    return repository
        .findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletIdAndNamespaceIn(
            SettingLevel.OUTLET, tenantId.value(), outletId.value(), namespaces);
  }

  private List<SettingEntity> loadForTerminal(
      TenantId tenantId, TerminalId terminalId, List<String> namespaces) {
    if (namespaces.isEmpty()) {
      return repository.findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndTerminalId(
          SettingLevel.TERMINAL, tenantId.value(), terminalId.value());
    }
    return repository
        .findByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndTerminalIdAndNamespaceIn(
            SettingLevel.TERMINAL, tenantId.value(), terminalId.value(), namespaces);
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
}
