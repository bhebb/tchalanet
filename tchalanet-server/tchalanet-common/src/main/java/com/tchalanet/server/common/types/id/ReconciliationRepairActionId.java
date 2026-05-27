package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for ReconciliationRepairAction. */
public record ReconciliationRepairActionId(UUID value) {

  public ReconciliationRepairActionId {
    if (value == null) throw new IllegalArgumentException("ReconciliationRepairActionId.value is null");
  }

  public static ReconciliationRepairActionId of(UUID value) {
    return new ReconciliationRepairActionId(value);
  }

  public static ReconciliationRepairActionId nullableOf(UUID raw) {
    return raw == null ? null : new ReconciliationRepairActionId(raw);
  }

  public static ReconciliationRepairActionId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("ReconciliationRepairActionId string is required");
    }
    return new ReconciliationRepairActionId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}

