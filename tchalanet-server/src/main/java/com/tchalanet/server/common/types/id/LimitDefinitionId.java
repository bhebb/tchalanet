package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for LimitDefinition. */
public record LimitDefinitionId(UUID value) {

  public LimitDefinitionId {
    if (value == null) throw new IllegalArgumentException("LimitDefinitionId.value is null");
  }

  public static LimitDefinitionId of(UUID value) {
    return new LimitDefinitionId(value);
  }

  /** Convenience for mappers: returns null if raw is null. */
  public static LimitDefinitionId nullableOf(UUID raw) {
    return raw == null ? null : new LimitDefinitionId(raw);
  }

  /** Parse from UUID string (web/input). */
  public static LimitDefinitionId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("LimitDefinitionId string is required");
    }
    return new LimitDefinitionId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
