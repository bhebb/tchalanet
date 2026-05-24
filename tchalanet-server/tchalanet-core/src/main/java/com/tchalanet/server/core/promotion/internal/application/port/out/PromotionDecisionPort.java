package com.tchalanet.server.core.promotion.internal.application.port.out;

import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import java.util.Optional;

public interface PromotionDecisionPort {
    Optional<PromotionDecision> findByContextHashAndPhase(String contextHash, String phase);
    PromotionDecision save(PromotionDecision decision);
}
