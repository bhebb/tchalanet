package com.tchalanet.server.core.limitpolicy.api.query;

import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
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
