package com.tchalanet.server.core.offlinesync.application.command.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.util.Map;
import java.util.UUID;

public record QueueOfflineCancelCommand(
    TenantId tenantId, UUID deviceId, Map<String, Object> payload) {}
