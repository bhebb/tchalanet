package com.tchalanet.server.core.autonomy.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.autonomy.api.AutonomyTargetType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.autonomy.internal.domain.model.AutonomyTargetId;

public record DeleteAutonomyRuleCommand(TenantId tenantId, AutonomyTargetType targetType, AutonomyTargetId targetId) implements Command<Void> {
}

