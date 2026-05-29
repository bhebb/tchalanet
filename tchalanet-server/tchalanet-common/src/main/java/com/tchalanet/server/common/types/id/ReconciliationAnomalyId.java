package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for ReconciliationAnomaly. */
public record ReconciliationAnomalyId(UUID value) {

  public ReconciliationAnomalyId {
    if (value == null) throw new IllegalArgumentException("ReconciliationAnomalyId.value is null");
  }

  public static ReconciliationAnomalyId of(UUID value) {
    return new ReconciliationAnomalyId(value);
  }

  public static ReconciliationAnomalyId nullableOf(UUID raw) {
    return raw == null ? null : new ReconciliationAnomalyId(raw);
  }

  public static ReconciliationAnomalyId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("ReconciliationAnomalyId string is required");
    }
    return new ReconciliationAnomalyId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}

