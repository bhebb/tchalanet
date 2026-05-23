package com.tchalanet.server.core.promotion.internal.application.port.out;

import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationContext;
import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRuleDefinition;
import java.util.List;

public interface PromotionRuleReaderPort {
  List<PromotionRuleDefinition> findCandidates(PromotionEvaluationContext context);
}
