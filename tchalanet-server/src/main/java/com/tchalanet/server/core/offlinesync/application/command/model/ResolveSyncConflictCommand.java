package com.tchalanet.server.core.offlinesync.application.command.model;

import java.util.UUID;
import java.util.Map;

public record ResolveSyncConflictCommand(UUID tenantId, UUID deviceId, UUID conflictId, Map<String,Object> resolution) {}

