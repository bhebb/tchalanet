package com.tchalanet.server.core.pos.application.command.model;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;
import java.util.Map;

public record RegisterPosDeviceCommand(
    TenantId tenantId,
    OutletId outletId,
    UUID deviceId,
    String label,
    Map<String, Object> capabilities
) implements Command<UUID> {}
