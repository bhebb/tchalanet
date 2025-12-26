package com.tchalanet.server.common.settings.registry;

import com.tchalanet.server.common.settings.AppSettingValueType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AppSettingValidator {

  public static void validateOrThrow(
      String namespace, String key, AppSettingValueType type, String value) {
    String full = namespace + "." + key;

    var reg = AppSettingRegistry.byFullKey().get(full);
    if (reg == null) {
      throw new IllegalArgumentException("Unknown app_setting key: " + full);
    }
    if (reg.type() != type) {
      throw new IllegalArgumentException(
          "Wrong value_type for " + full + ": expected " + reg.type() + ", got " + type);
    }
    // parse check
    parse(type, value);
  }

  private static void parse(AppSettingValueType type, String value) {
    try {
      switch (type) {
        case BOOLEAN -> Boolean.parseBoolean(value);
        case INT -> Integer.parseInt(value);
        case LONG -> Long.parseLong(value);
        case DECIMAL -> new java.math.BigDecimal(value);
        case JSON -> new com.fasterxml.jackson.databind.ObjectMapper().readTree(value);
        case STRING -> {
          /* ok */
        }
      }
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid value for type " + type + ": " + value, ex);
    }
  }
}
