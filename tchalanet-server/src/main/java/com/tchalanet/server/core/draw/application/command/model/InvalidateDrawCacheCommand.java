package com.tchalanet.server.core.draw.application.command.model;

import java.util.UUID;

public record InvalidateDrawCacheCommand(UUID tenantId) {}
