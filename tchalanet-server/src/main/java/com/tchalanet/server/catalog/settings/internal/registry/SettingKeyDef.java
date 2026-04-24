package com.tchalanet.server.catalog.settings.internal.registry;

import com.tchalanet.server.catalog.settings.api.model.SettingValueType;

/**
 * Setting Key Definition (INTERNAL)
 *
 * <p>Type-safe definition of a setting with its namespace, key, type, and default value.
 *
 * @param namespace setting namespace
 * @param key setting key
 * @param type value type
 * @param defaultValue default value (typed)
 * @param <T> value type
 */
public record SettingKeyDef<T>(
    String namespace, String key, SettingValueType type, T defaultValue) {

  /** Full key in format "namespace.key" */
  public String fullKey() {
    return namespace + "." + key;
  }
}
