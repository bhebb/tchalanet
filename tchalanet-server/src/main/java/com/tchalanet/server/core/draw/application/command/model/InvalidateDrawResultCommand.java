package com.tchalanet.server.core.draw.application.command.model;

import java.time.Instant;
import java.util.UUID;

public record InvalidateDrawResultCommand(
    UUID drawId, UUID tenantId, UUID performedBy, Instant performedAt, String reason) {}
