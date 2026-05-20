package com.tchalanet.server.common.types.id;

import java.util.UUID;

/**
 * Typed identifier for I18n Override.
 *
 * <p>This wrapper prevents UUID mixups and makes method signatures self-documenting.
 */
public record I18nOverrideId(UUID value) {

  public I18nOverrideId {
    if (value == null) {
      throw new IllegalArgumentException("I18nOverrideId.value is null");
    }
  }

  public static I18nOverrideId of(UUID value) {
    return new I18nOverrideId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static I18nOverrideId nullableOf(UUID raw) {
    return raw == null ? null : new I18nOverrideId(raw);
  }

  /** Parse from UUID string input (web/request). */
  public static I18nOverrideId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("I18nOverrideId string is required");
    }
    return new I18nOverrideId(UUID.fromString(raw));
  }
}
