package com.tchalanet.server.common.settings.registry;

import com.tchalanet.server.common.settings.AppSettingValueType;

public record AppSettingKey<T>(
    String namespace, String key, AppSettingValueType type, T defaultValue) {
  public String fullKey() {
    return namespace + "." + key;
  }
}
