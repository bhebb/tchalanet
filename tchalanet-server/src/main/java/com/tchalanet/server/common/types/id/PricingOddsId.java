package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for Pricing Odds entries */
public record PricingOddsId(UUID value) {

  public PricingOddsId {
    if (value == null) throw new IllegalArgumentException("PricingOddsId.value is null");
  }

  public static PricingOddsId of(UUID v) { return new PricingOddsId(v); }
  public static PricingOddsId nullableOf(UUID v) { return v == null ? null : new PricingOddsId(v); }
  public static PricingOddsId parse(String s) { return new PricingOddsId(UUID.fromString(s)); }
}
