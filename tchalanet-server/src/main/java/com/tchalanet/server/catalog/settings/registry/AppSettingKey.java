package com.tchalanet.server.catalog.settings.registry;

import com.tchalanet.server.catalog.settings.AppSettingValueType;

public record AppSettingKey<T>(
    String namespace, String key, AppSettingValueType type, T defaultValue) {
  public String fullKey() {
    return namespace + "." + key;
  }
}
