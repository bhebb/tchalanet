package com.tchalanet.server.core.pos.application.command.model;

import java.util.UUID;

public record UnregisterPosDeviceCommand(UUID tenantId, UUID deviceId) {}

