package com.tchalanet.server.catalog.settings.api.model;

import com.tchalanet.server.catalog.settings.api.SettingsCatalog;

/**
 * Resolved Setting View
 *
 * <p>Represents a setting after hierarchical resolution. Shows which level provided the effective
 * value.
 *
 * <p>This is the output format for {@link SettingsCatalog#resolve}.
 *
 * @param namespace setting namespace
 * @param settingKey setting key within namespace
 * @param valueType declared type
 * @param settingValue effective value (as text)
 * @param effectiveLevel which level provided this value (GLOBAL, TENANT, OUTLET, TERMINAL)
 */
public record ResolvedSettingView(
    String namespace,
    String settingKey,
    SettingValueType valueType,
    String settingValue,
    SettingLevel effectiveLevel) {

  /** Full key in format "namespace.settingKey" */
  public String fullKey() {
    return namespace + "." + settingKey;
  }
}
