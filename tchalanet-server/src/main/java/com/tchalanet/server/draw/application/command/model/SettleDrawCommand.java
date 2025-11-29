package com.tchalanet.server.draw.application.command.model;

import java.util.UUID;

public record SettleDrawCommand(UUID tenantId, UUID drawId) {}
