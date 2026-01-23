package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for ResultSlot. */
public record ResultSlotId(UUID value) {

  public ResultSlotId {
    if (value == null) throw new IllegalArgumentException("ResultSlotId.value is null");
  }

  public static ResultSlotId of(UUID value) {
    return new ResultSlotId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static ResultSlotId nullableOf(UUID raw) {
    return raw == null ? null : new ResultSlotId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static ResultSlotId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("ResultSlotId string is required");
    }
    return new ResultSlotId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
