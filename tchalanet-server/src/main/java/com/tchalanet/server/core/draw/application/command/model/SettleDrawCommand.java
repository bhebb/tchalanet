package com.tchalanet.server.core.draw.application.command.model;

import java.util.UUID;

public record SettleDrawCommand(UUID tenantId, UUID drawId) {}
