package com.tchalanet.server.core.autonomy.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyTargetId;

public record DeleteAutonomyRuleCommand(TenantId tenantId, AutonomyTargetType targetType, AutonomyTargetId targetId) implements Command<Void> {
}

