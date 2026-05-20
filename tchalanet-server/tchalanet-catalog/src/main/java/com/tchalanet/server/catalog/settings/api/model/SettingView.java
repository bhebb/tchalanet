package com.tchalanet.server.catalog.settings.api.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SettingId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;

/**
 * Setting View (Read Model)
 *
 * <p>Represents a single setting entry at a specific level. This is the admin view that shows the
 * raw configuration entry.
 *
 * <p>For resolved/merged settings, use {@link ResolvedSettingView}.
 *
 * @param id unique identifier
 * @param namespace setting namespace (e.g., "pos.behavior", "ticket.verification")
 * @param settingKey setting key within namespace (e.g., "require_open_session")
 * @param settingValue value as text (always stored as string)
 * @param valueType declared type for parsing/validation
 * @param level scope level (GLOBAL, TENANT, OUTLET, TERMINAL)
 * @param tenantId tenant context (null for GLOBAL)
 * @param outletId outlet context (null unless OUTLET level)
 * @param terminalId terminal context (null unless TERMINAL level)
 * @param active whether this setting is active
 */
public record SettingView(
    SettingId id,
    String namespace,
    String settingKey,
    String settingValue,
    SettingValueType valueType,
    SettingLevel level,
    TenantId tenantId,
    OutletId outletId,
    TerminalId terminalId,
    Boolean active) {

  /** Full key in format "namespace.settingKey" */
  public String fullKey() {
    return namespace + "." + settingKey;
  }
}
