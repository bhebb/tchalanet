package com.tchalanet.server.core.limitpolicy.application.command.model;

import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.common.types.enums.RuleKey;

public record UpsertLimitDefinitionResult(
    LimitDefinitionId id,
    RuleKey ruleKey
) {}
