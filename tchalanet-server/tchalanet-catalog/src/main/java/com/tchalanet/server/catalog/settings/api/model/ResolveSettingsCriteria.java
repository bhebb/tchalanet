package com.tchalanet.server.catalog.settings.api.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.util.List;

/**
 * Resolve Settings Criteria
 *
 * <p>Criteria for resolving settings with hierarchical merge (GLOBAL → TENANT).
 *
 * @param tenantId tenant context (required)
 * @param namespaces namespace filter (empty = all namespaces)
 */
public record ResolveSettingsCriteria(TenantId tenantId, List<String> namespaces) {

  /**
   * Create criteria for tenant-only resolution.
   *
   * @param tenantId tenant ID (required)
   * @param namespaces namespace filter
   * @return criteria
   */
  public static ResolveSettingsCriteria forTenant(TenantId tenantId, List<String> namespaces) {
    return new ResolveSettingsCriteria(tenantId, namespaces);
  }
}
