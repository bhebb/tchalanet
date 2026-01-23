package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for ThemePreset. */
public record ThemePresetId(UUID value) {

  public ThemePresetId {
    if (value == null) throw new IllegalArgumentException("ThemePresetId.value is null");
  }

  public static ThemePresetId of(UUID value) {
    return new ThemePresetId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static ThemePresetId nullableOf(UUID raw) {
    return raw == null ? null : new ThemePresetId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static ThemePresetId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("ThemePresetId string is required");
    }
    return new ThemePresetId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
