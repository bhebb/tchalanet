package com.tchalanet.server.core.limitpolicy.domain.model;

import java.util.Map;

public record ResolvedLimitSet(
    Map<RuleKey, LimitDefinition> limits
) {}
