package com.tchalanet.server.core.tenant.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.enums.TenantType;

import java.util.UUID;

public record CreateTenantCommand(
    String code,
    String name,
    TenantType type,
    String timezone,
    String currency
) implements Command<UUID> {
}
