package com.tchalanet.server.core.pos.application.command.model;

import java.util.UUID;
import java.util.Map;

public record RegisterPosDeviceCommand(
    UUID tenantId,
    UUID outletId,
    UUID deviceId,
    String label,
    Map<String, Object> capabilities
) {}
