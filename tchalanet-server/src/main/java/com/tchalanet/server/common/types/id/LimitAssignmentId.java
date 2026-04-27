package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for LimitAssignment. */
public record LimitAssignmentId(UUID value) {

  public LimitAssignmentId {
    if (value == null) throw new IllegalArgumentException("LimitAssignmentId.value is null");
  }

  public static LimitAssignmentId of(UUID value) {
    return new LimitAssignmentId(value);
  }

  public static LimitAssignmentId nullableOf(UUID raw) {
    return raw == null ? null : new LimitAssignmentId(raw);
  }

  public static LimitAssignmentId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("LimitAssignmentId string is required");
    }
    return new LimitAssignmentId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
