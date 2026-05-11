package com.tchalanet.server.core.limitpolicy.application.query.model.rules;

import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import tools.jackson.databind.JsonNode;

public record LimitRuleSpec(
    RuleKey ruleKey,
    String label,
    String description,
    BreachOutcome defaultOutcome,
    String category,
    boolean stateless,
    JsonNode paramsTemplate
) {
}
