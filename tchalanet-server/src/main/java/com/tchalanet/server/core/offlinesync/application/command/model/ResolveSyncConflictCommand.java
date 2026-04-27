package com.tchalanet.server.core.offlinesync.application.command.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.util.Map;
import java.util.UUID;

public record ResolveSyncConflictCommand(
    TenantId tenantId, UUID deviceId, UUID conflictId, Map<String, Object> resolution) {}
