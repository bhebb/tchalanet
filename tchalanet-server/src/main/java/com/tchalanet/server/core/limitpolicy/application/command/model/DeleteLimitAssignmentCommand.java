package com.tchalanet.server.core.limitpolicy.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.enums.TargetType;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.UUID;

public record DeleteLimitAssignmentCommand(
    TenantId tenantId, UUID assignmentId, TargetType targetType, UUID targetId)
    implements Command<Void> {}
