package com.tchalanet.server.core.limitpolicy.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.limitpolicy.domain.model.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import com.tchalanet.server.core.limitpolicy.domain.model.RuleKey;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateLimitDefinitionCommand(
    UUID tenantId,
    RuleKey ruleKey,
    boolean enabled,
    BreachOutcome onBreach,
    Map<String, Object> params,
    List<String> betTypes,
    String selectionPattern
) implements Command<LimitDefinition> {}
