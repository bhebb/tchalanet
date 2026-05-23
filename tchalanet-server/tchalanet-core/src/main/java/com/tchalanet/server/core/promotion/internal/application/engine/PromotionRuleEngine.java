package com.tchalanet.server.core.promotion.internal.application.engine;

import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRuleDefinition;
import java.util.List;

public interface PromotionRuleEngine {
  PromotionDecision evaluate(List<PromotionRuleDefinition> rules, PromotionEvaluationContext context);
}
