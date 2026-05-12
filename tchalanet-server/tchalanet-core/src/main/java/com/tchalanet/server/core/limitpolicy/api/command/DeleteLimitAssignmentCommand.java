package com.tchalanet.server.core.limitpolicy.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import jakarta.validation.constraints.NotNull;

public record DeleteLimitAssignmentCommand(
    @NotNull LimitAssignmentId id
) implements Command<DeleteLimitAssignmentResult> {}
