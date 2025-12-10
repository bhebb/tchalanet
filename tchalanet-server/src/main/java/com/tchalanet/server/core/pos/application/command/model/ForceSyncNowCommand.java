package com.tchalanet.server.core.pos.application.command.model;

import java.util.UUID;

public record ForceSyncNowCommand(UUID tenantId, UUID deviceId) {}

