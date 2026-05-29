package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record AppliedPromotionSnapshotId(UUID value) {
  public AppliedPromotionSnapshotId {
    if (value == null) {
      throw new IllegalArgumentException("AppliedPromotionSnapshotId.value is null");
    }
  }

  public static AppliedPromotionSnapshotId of(UUID value) { return new AppliedPromotionSnapshotId(value); }
  public static AppliedPromotionSnapshotId nullableOf(UUID raw) { return raw == null ? null : new AppliedPromotionSnapshotId(raw); }
  public static AppliedPromotionSnapshotId parse(String raw) {
    if (raw == null || raw.isBlank()) throw new IllegalArgumentException("AppliedPromotionSnapshotId string is required");
    return new AppliedPromotionSnapshotId(UUID.fromString(raw));
  }
}
