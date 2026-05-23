package com.tchalanet.server.core.promotion.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.query.EvaluatePromotionsQuery;
import com.tchalanet.server.core.promotion.internal.application.engine.PromotionRuleEngine;
import com.tchalanet.server.core.promotion.internal.application.port.out.PromotionRuleReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class EvaluatePromotionsQueryHandler implements QueryHandler<EvaluatePromotionsQuery, PromotionDecision> {

  private final PromotionRuleReaderPort reader;
  private final PromotionRuleEngine engine;

  @Override
  public PromotionDecision handle(EvaluatePromotionsQuery q) {
    var rules = reader.findCandidates(q.context());
    return engine.evaluate(rules, q.context());
  }
}
