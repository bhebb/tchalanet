package com.tchalanet.server.catalog.settings.internal.registry;

import com.tchalanet.server.catalog.settings.api.model.SettingExposure;
import com.tchalanet.server.catalog.settings.api.model.SettingValueType;

/**
 * Setting Key Definition (INTERNAL)
 *
 * <p>defaultExposure: canonical exposure for this key. Defaults to INTERNAL.
 * exposureOverridable: if false, admin cannot change the exposure of this key.
 */
public record SettingKeyDef<T>(
    String namespace,
    String key,
    SettingValueType type,
    T defaultValue,
    SettingExposure defaultExposure,
    boolean exposureOverridable) {

  public SettingKeyDef(String namespace, String key, SettingValueType type, T defaultValue) {
    this(namespace, key, type, defaultValue, SettingExposure.INTERNAL, true);
  }

  public String fullKey() {
    return namespace + "." + key;
  }
}
