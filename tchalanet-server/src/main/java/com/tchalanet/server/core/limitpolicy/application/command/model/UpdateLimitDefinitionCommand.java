package com.tchalanet.server.core.limitpolicy.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UpdateLimitDefinitionCommand(
    UUID definitionId,
    boolean enabled,
    BreachOutcome onBreach,
    Map<String, Object> params,
    List<String> betTypes,
    String selectionPattern
) implements Command<LimitDefinition> {}
