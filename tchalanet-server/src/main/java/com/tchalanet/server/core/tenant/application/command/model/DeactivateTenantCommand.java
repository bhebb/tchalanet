package com.tchalanet.server.core.tenant.application.command.model;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Command;

import java.util.UUID;

public record DeactivateTenantCommand(TenantId tenantId, String reason) implements Command<Void> {} // -> suspend

