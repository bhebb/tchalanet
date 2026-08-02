package com.tchalanet.server.core.sales.api.model.settlement;

import java.util.List;
import java.util.Objects;

/** Immutable record of the settlement term(s) that actually produced a payout. */
public record AppliedSettlementSnapshot(
    int schemaVersion, List<AppliedSettlementTermSnapshot> terms) {

  public static final int CURRENT_SCHEMA_VERSION = 1;

  public AppliedSettlementSnapshot {
    if (schemaVersion <= 0) {
      throw new IllegalArgumentException("applied_settlement.schema_version_invalid");
    }
    terms = List.copyOf(Objects.requireNonNull(terms, "applied_settlement.terms_required"));
    if (terms.isEmpty()) {
      throw new IllegalArgumentException("applied_settlement.terms_required");
    }
  }

  public static AppliedSettlementSnapshot current(List<AppliedSettlementTermSnapshot> terms) {
    return new AppliedSettlementSnapshot(CURRENT_SCHEMA_VERSION, terms);
  }
}
