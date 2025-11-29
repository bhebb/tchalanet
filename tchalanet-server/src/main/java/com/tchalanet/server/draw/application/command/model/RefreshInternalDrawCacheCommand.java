package com.tchalanet.server.draw.application.command.model;

import java.util.UUID;

public record RefreshInternalDrawCacheCommand(UUID tenantId, UUID triggeredBy) {}
