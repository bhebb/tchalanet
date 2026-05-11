package com.tchalanet.server.core.limitpolicy.domain.rule;

import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.core.limitpolicy.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitBreach;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitFactsSnapshot;

import java.util.List;

public interface LimitRuleEvaluator {

    RuleKey supports();

    List<LimitBreach> evaluate(
        EffectiveLimitRule rule,
        LimitFactsSnapshot facts,
        LimitContext context
    );
}
