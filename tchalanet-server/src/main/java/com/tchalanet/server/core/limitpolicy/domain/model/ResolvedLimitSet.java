package com.tchalanet.server.core.limitpolicy.domain.model;

import com.tchalanet.server.common.types.enums.RuleKey;
import java.util.Map;

public record ResolvedLimitSet(Map<RuleKey, LimitDefinition> limits) {}
