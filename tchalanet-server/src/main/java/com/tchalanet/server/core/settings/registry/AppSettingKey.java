package com.tchalanet.server.core.settings.registry;

import com.tchalanet.server.core.settings.AppSettingValueType;

public record AppSettingKey<T>(
    String namespace, String key, AppSettingValueType type, T defaultValue) {
  public String fullKey() {
    return namespace + "." + key;
  }
}
