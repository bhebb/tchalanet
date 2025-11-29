package com.tchalanet.server.draw.application.command.model;

import java.util.UUID;

public record RefreshPublicDrawsCacheCommand(UUID tenantId, UUID triggeredBy) {}
