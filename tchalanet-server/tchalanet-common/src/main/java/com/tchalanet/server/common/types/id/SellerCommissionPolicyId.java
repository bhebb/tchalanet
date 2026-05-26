package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record SellerCommissionPolicyId(UUID value) {
  public SellerCommissionPolicyId {
    if (value == null) {
      throw new IllegalArgumentException("SellerCommissionPolicyId.value is null");
    }
  }

  public static SellerCommissionPolicyId of(UUID value) { return new SellerCommissionPolicyId(value); }
  public static SellerCommissionPolicyId nullableOf(UUID raw) { return raw == null ? null : new SellerCommissionPolicyId(raw); }
  public static SellerCommissionPolicyId parse(String raw) {
    if (raw == null || raw.isBlank()) throw new IllegalArgumentException("SellerCommissionPolicyId string is required");
    return new SellerCommissionPolicyId(UUID.fromString(raw));
  }
}
