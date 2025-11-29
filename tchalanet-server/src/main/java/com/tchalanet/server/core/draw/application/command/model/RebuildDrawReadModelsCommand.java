package com.tchalanet.server.core.draw.application.command.model;

import java.util.UUID;

public record RebuildDrawReadModelsCommand(UUID tenantId, UUID triggeredBy) {}
