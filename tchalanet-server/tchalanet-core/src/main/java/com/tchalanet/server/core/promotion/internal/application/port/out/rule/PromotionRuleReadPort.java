package com.tchalanet.server.core.promotion.internal.application.port.out;

import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRule;
import com.tchalanet.server.core.promotion.api.model.PromotionEvaluationPhase;
import java.util.List;

public interface PromotionRuleReadPort {
    List<PromotionRule> findActiveRulesForPhase(PromotionEvaluationPhase phase);
}

