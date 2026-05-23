package com.tchalanet.server.core.promotion.api.model;

import java.util.List;

public record PromotionDecision(
    List<FreeLineGrant> freeLineGrants,
    List<PayoutModifier> payoutModifiers,
    List<DiscountModifier> discountModifiers,
    List<CommissionModifier> commissionModifiers,
    List<PromotionNotice> notices,
    List<AppliedPromotionSnapshot> snapshots
) {
  public static PromotionDecision empty() {
    return new PromotionDecision(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
  }
}
