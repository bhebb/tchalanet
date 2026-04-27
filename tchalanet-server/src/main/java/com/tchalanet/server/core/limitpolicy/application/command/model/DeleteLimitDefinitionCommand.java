package com.tchalanet.server.core.limitpolicy.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import jakarta.validation.constraints.NotNull;

public record DeleteLimitDefinitionCommand(
    @NotNull LimitDefinitionId id
) implements Command<com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitDefinitionResult> {}
