package com.tchalanet.server.common.types.id;

import java.util.UUID;

/**
 * Typed identifier for Setting.
 *
 * <p>This wrapper prevents UUID mixups and makes method signatures self-documenting.
 */
public record SettingId(UUID value) {

  public SettingId {
    if (value == null) {
      throw new IllegalArgumentException("SettingId.value is null");
    }
  }

  public static SettingId of(UUID value) {
    return new SettingId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static SettingId nullableOf(UUID raw) {
    return raw == null ? null : new SettingId(raw);
  }

  /** Parse from UUID string input (web/request). */
  public static SettingId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("SettingId string is required");
    }
    return new SettingId(UUID.fromString(raw));
  }
}
