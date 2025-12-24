package com.tchalanet.server.core.limitpolicy.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.limitpolicy.domain.model.TargetType;

import java.util.UUID;

public record DeleteLimitAssignmentCommand(
    UUID tenantId,
    UUID assignmentId,
    TargetType targetType,
    UUID targetId
) implements Command<Void> {}
