package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record OfflineCodeBatchId(UUID value) {

  public OfflineCodeBatchId {
    if (value == null) {
      throw new IllegalArgumentException("OfflineCodeBatchId.value is null");
    }
  }

  public static OfflineCodeBatchId of(UUID value) {
    return new OfflineCodeBatchId(value);
  }

  public static OfflineCodeBatchId nullableOf(UUID value) {
    return value == null ? null : new OfflineCodeBatchId(value);
  }

  public static OfflineCodeBatchId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("OfflineCodeBatchId string is required");
    }
    return new OfflineCodeBatchId(UUID.fromString(raw));
  }
}
