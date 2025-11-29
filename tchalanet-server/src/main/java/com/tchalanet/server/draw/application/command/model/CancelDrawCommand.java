package com.tchalanet.server.draw.application.command.model;

import java.util.UUID;

public record CancelDrawCommand(UUID tenantId, UUID drawId, UUID cancelledBy, String reason) {}
