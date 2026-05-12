package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record ThemePresetId(UUID value) {
  public ThemePresetId {
    if (value == null) throw new IllegalArgumentException("ThemePresetId value is null");
  }

  public UUID uuid() {
    return value;
  }

  public static ThemePresetId of(UUID value) {
    return new ThemePresetId(value);
  }

  public static ThemePresetId nullableOf(UUID value) {
    return value == null ? null : new ThemePresetId(value);
  }

  public static ThemePresetId parse(String value) {
    return value == null ? null : new ThemePresetId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
