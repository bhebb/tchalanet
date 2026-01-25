package com.tchalanet.server.catalog.settings.api;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.util.List;

/**
 * Resolve Settings Criteria
 *
 * <p>Criteria for resolving settings with hierarchical merge (GLOBAL → TENANT → OUTLET →
 * TERMINAL).
 *
 * @param tenantId tenant context (required)
 * @param outletId outlet context (optional, for OUTLET-level overrides)
 * @param terminalId terminal context (optional, for TERMINAL-level overrides)
 * @param namespaces namespace filter (empty = all namespaces)
 */
public record ResolveSettingsCriteria(
    TenantId tenantId, OutletId outletId, TerminalId terminalId, List<String> namespaces) {

  /**
   * Create criteria for tenant-only resolution.
   *
   * @param tenantId tenant ID (required)
   * @param namespaces namespace filter
   * @return criteria
   */
  public static ResolveSettingsCriteria forTenant(TenantId tenantId, List<String> namespaces) {
    return new ResolveSettingsCriteria(tenantId, null, null, namespaces);
  }

  /**
   * Create criteria for outlet resolution.
   *
   * @param tenantId tenant ID (required)
   * @param outletId outlet ID (required)
   * @param namespaces namespace filter
   * @return criteria
   */
  public static ResolveSettingsCriteria forOutlet(
      TenantId tenantId, OutletId outletId, List<String> namespaces) {
    return new ResolveSettingsCriteria(tenantId, outletId, null, namespaces);
  }

  /**
   * Create criteria for terminal resolution.
   *
   * @param tenantId tenant ID (required)
   * @param terminalId terminal ID (required)
   * @param namespaces namespace filter
   * @return criteria
   */
  public static ResolveSettingsCriteria forTerminal(
      TenantId tenantId, TerminalId terminalId, List<String> namespaces) {
    return new ResolveSettingsCriteria(tenantId, null, terminalId, namespaces);
  }
}
