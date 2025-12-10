package com.tchalanet.server.core.pos.application.command.model;

import java.util.UUID;

public record LockPosDeviceCommand(UUID tenantId, UUID deviceId, UUID requestedBy) {}

