package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for ReconciliationRun. */
public record ReconciliationRunId(UUID value) {

  public ReconciliationRunId {
    if (value == null) throw new IllegalArgumentException("ReconciliationRunId.value is null");
  }

  public static ReconciliationRunId of(UUID value) {
    return new ReconciliationRunId(value);
  }

  public static ReconciliationRunId nullableOf(UUID raw) {
    return raw == null ? null : new ReconciliationRunId(raw);
  }

  public static ReconciliationRunId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("ReconciliationRunId string is required");
    }
    return new ReconciliationRunId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}

