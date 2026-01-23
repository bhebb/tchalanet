package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for DrawResult. */
public record DrawResultId(UUID value) {

  public DrawResultId {
    if (value == null) throw new IllegalArgumentException("DrawResultId.value is null");
  }

  public static DrawResultId of(UUID value) {
    return new DrawResultId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static DrawResultId nullableOf(UUID raw) {
    return raw == null ? null : new DrawResultId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static DrawResultId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("DrawResultId string is required");
    }
    return new DrawResultId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
