package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record OfflineSaleSubmissionId(UUID value) {

  public OfflineSaleSubmissionId {
    if (value == null) {
      throw new IllegalArgumentException("OfflineSaleSubmissionId.value is null");
    }
  }

  public static OfflineSaleSubmissionId of(UUID value) {
    return new OfflineSaleSubmissionId(value);
  }

  public static OfflineSaleSubmissionId nullableOf(UUID value) {
    return value == null ? null : new OfflineSaleSubmissionId(value);
  }

  public static OfflineSaleSubmissionId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("OfflineSaleSubmissionId string is required");
    }
    return new OfflineSaleSubmissionId(UUID.fromString(raw));
  }
}
