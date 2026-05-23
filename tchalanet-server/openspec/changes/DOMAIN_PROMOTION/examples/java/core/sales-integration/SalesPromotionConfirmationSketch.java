package com.tchalanet.server.core.sales.integration;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.promotion.api.query.EvaluatePromotionsQuery;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SalesPromotionConfirmationSketch {

  private final QueryBus queryBus;

  public void confirm(/* SellTicketCommand c */) {
    // 1. Rebuild PromotionEvaluationContext with phase SALE_CONFIRMATION.
    // 2. Evaluate promotions again.
    // 3. Reject client-forced free lines that are not present in decision.freeLineGrants().
    // 4. Store decision.snapshots() into ticket_line_applied_rule.
    // 5. Persist paid_amount and effective_stake_amount distinctly.
  }
}
