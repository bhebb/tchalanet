package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record OfflineSubmissionId(UUID value) {

  public OfflineSubmissionId {
    if (value == null) {
      throw new IllegalArgumentException("OfflineSubmissionId.value is null");
    }
  }

  public static OfflineSubmissionId of(UUID value) {
    return new OfflineSubmissionId(value);
  }

  public static OfflineSubmissionId nullableOf(UUID value) {
    return value == null ? null : new OfflineSubmissionId(value);
  }

  public static OfflineSubmissionId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("OfflineSubmissionId string is required");
    }
    return new OfflineSubmissionId(UUID.fromString(raw));
  }

  public UUID uuid() {
    return value;
  }
}
