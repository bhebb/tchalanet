package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record OfflineSyncBatchId(UUID value) {

  public OfflineSyncBatchId {
    if (value == null) {
      throw new IllegalArgumentException("OfflineSyncBatchId.value is null");
    }
  }

  public static OfflineSyncBatchId of(UUID value) {
    return new OfflineSyncBatchId(value);
  }

  public static OfflineSyncBatchId nullableOf(UUID value) {
    return value == null ? null : new OfflineSyncBatchId(value);
  }

  public static OfflineSyncBatchId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("OfflineSyncBatchId string is required");
    }
    return new OfflineSyncBatchId(UUID.fromString(raw));
  }

  public UUID uuid() {
    return value;
  }
}
