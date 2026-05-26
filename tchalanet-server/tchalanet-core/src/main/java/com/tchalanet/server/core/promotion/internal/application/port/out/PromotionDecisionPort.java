package com.tchalanet.server.core.promotion.internal.application.port.out;

import com.tchalanet.server.common.types.id.PromotionDecisionId;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import java.util.Optional;

public interface PromotionDecisionPort {
    Optional<PromotionDecision> findById(PromotionDecisionId decisionId);
    Optional<PromotionDecision> findByContextHashAndPhase(String contextHash, String phase);
    PromotionDecision save(PromotionDecision decision);
}
