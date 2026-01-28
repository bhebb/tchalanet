package com.tchalanet.server.features.tenantadmin.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record SuspendTenantUserCommand(TenantId tenantId, UserId userId, String reason) implements Command<Void> {}
