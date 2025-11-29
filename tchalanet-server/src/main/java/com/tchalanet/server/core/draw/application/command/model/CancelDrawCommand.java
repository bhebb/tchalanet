package com.tchalanet.server.core.draw.application.command.model;

import java.util.UUID;

public record CancelDrawCommand(UUID tenantId, UUID drawId, UUID cancelledBy, String reason) {}
