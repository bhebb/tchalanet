package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record OfflineBatchId(UUID value) {

  public OfflineBatchId {
    if (value == null) {
      throw new IllegalArgumentException("OfflineBatchId.value is null");
    }
  }

  public static OfflineBatchId of(UUID value) {
    return new OfflineBatchId(value);
  }

  public static OfflineBatchId nullableOf(UUID value) {
    return value == null ? null : new OfflineBatchId(value);
  }

  public static OfflineBatchId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("OfflineBatchId string is required");
    }
    return new OfflineBatchId(UUID.fromString(raw));
  }
}
