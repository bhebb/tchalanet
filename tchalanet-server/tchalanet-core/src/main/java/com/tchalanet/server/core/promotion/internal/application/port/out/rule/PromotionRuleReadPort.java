package com.tchalanet.server.core.promotion.internal.application.port.out.rule;

import com.tchalanet.server.core.promotion.internal.domain.model.PromotionRule;
import java.util.List;

public interface PromotionRuleReadPort {
    List<PromotionRule> findActiveRules();
}
