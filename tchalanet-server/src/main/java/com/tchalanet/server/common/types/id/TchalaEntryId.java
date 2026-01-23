package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for TchalaEntry. */
public record TchalaEntryId(UUID value) {

  public TchalaEntryId {
    if (value == null) throw new IllegalArgumentException("TchalaEntryId.value is null");
  }

  public static TchalaEntryId of(UUID value) {
    return new TchalaEntryId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static TchalaEntryId nullableOf(UUID raw) {
    return raw == null ? null : new TchalaEntryId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static TchalaEntryId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("TchalaEntryId string is required");
    }
    return new TchalaEntryId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
