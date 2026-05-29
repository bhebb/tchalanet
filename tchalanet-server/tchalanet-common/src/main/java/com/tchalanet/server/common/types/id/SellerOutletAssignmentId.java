package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record SellerOutletAssignmentId(UUID value) {
  public SellerOutletAssignmentId {
    if (value == null) {
      throw new IllegalArgumentException("SellerOutletAssignmentId.value is null");
    }
  }

  public static SellerOutletAssignmentId of(UUID value) { return new SellerOutletAssignmentId(value); }
  public static SellerOutletAssignmentId nullableOf(UUID raw) { return raw == null ? null : new SellerOutletAssignmentId(raw); }
  public static SellerOutletAssignmentId parse(String raw) {
    if (raw == null || raw.isBlank()) throw new IllegalArgumentException("SellerOutletAssignmentId string is required");
    return new SellerOutletAssignmentId(UUID.fromString(raw));
  }
}
