package com.tchalanet.server.features.tenantadmin.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.enums.AutonomyLevel;

public record ChangeTenantUserAutonomyCommand(TenantId tenantId, UserId userId, AutonomyLevel autonomyLevel) implements Command<Void> {}
