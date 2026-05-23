package com.tchalanet.server.core.sales.integration;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import com.tchalanet.server.core.promotion.api.model.PromotionPhase;
import com.tchalanet.server.core.promotion.api.query.EvaluatePromotionsQuery;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SalesPromotionPreviewSketch {

  private final QueryBus queryBus;

  public void preview(/* SellPreviewCommand c */) {
    PromotionEvaluationContext ctx = null; // build from operational context + cart + draw
    var decision = queryBus.ask(new EvaluatePromotionsQuery(ctx));

    // Merge decision.notices into ApiResponse notices.
    // Offer decision.freeLineGrants to UI.
    // Include decision.payoutModifiers in preview explanation.
  }
}
