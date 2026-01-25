package com.tchalanet.server.catalog.settings.api;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.util.List;

/**
 * Settings Catalog - Read Contract
 *
 * <p>Provides read-only access to application settings with hierarchical resolution:
 * GLOBAL → TENANT → OUTLET → TERMINAL
 *
 * <p>Settings are merged in order of specificity, with later levels overriding earlier ones.
 *
 * <p>This is the ONLY public interface for reading settings. All consumers (core, features) must
 * use this API.
 *
 * @see SettingView
 * @see ResolvedSettingView
 */
public interface SettingsCatalog {

  /**
   * Resolve effective settings for a tenant context.
   *
   * <p>Merges settings in order: GLOBAL → TENANT → OUTLET (if provided) → TERMINAL (if provided)
   *
   * @param tenantId tenant context (required)
   * @param outletId outlet context (optional)
   * @param terminalId terminal context (optional)
   * @param namespaces filter by namespaces (empty = all namespaces)
   * @return list of resolved settings with effective level
   */
  List<ResolvedSettingView> resolve(
      TenantId tenantId, OutletId outletId, TerminalId terminalId, List<String> namespaces);

  /**
   * Resolve effective settings for a tenant (no outlet/terminal context).
   *
   * @param tenantId tenant context (required)
   * @param namespaces filter by namespaces (empty = all namespaces)
   * @return list of resolved settings with effective level
   */
  default List<ResolvedSettingView> resolveForTenant(TenantId tenantId, List<String> namespaces) {
    return resolve(tenantId, null, null, namespaces);
  }

  /**
   * Resolve effective settings for an outlet context.
   *
   * @param tenantId tenant context (required)
   * @param outletId outlet context (required)
   * @param namespaces filter by namespaces (empty = all namespaces)
   * @return list of resolved settings with effective level
   */
  default List<ResolvedSettingView> resolveForOutlet(
      TenantId tenantId, OutletId outletId, List<String> namespaces) {
    return resolve(tenantId, outletId, null, namespaces);
  }

  /**
   * Resolve effective settings for a terminal context.
   *
   * @param tenantId tenant context (required)
   * @param terminalId terminal context (required)
   * @param namespaces filter by namespaces (empty = all namespaces)
   * @return list of resolved settings with effective level
   */
  default List<ResolvedSettingView> resolveForTerminal(
      TenantId tenantId, TerminalId terminalId, List<String> namespaces) {
    return resolve(tenantId, null, terminalId, namespaces);
  }
}
