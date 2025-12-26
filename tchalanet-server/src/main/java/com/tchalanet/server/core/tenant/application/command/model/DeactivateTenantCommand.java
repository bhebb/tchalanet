package com.tchalanet.server.core.tenant.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;

public record DeactivateTenantCommand(TenantId tenantId, String reason)
    implements Command<Void> {} // -> suspend
