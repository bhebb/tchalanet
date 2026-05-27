package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for ReconciliationCheckResult. */
public record ReconciliationCheckResultId(UUID value) {

  public ReconciliationCheckResultId {
    if (value == null) throw new IllegalArgumentException("ReconciliationCheckResultId.value is null");
  }

  public static ReconciliationCheckResultId of(UUID value) {
    return new ReconciliationCheckResultId(value);
  }

  public static ReconciliationCheckResultId nullableOf(UUID raw) {
    return raw == null ? null : new ReconciliationCheckResultId(raw);
  }

  public static ReconciliationCheckResultId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("ReconciliationCheckResultId string is required");
    }
    return new ReconciliationCheckResultId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}

