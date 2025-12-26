package com.tchalanet.server.core.pos.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.Map;
import java.util.UUID;

public record RegisterPosDeviceCommand(
    TenantId tenantId,
    OutletId outletId,
    UUID deviceId,
    String label,
    Map<String, Object> capabilities)
    implements Command<UUID> {}
