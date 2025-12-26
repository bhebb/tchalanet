package com.tchalanet.server.core.offlinesync.application.command.model;
import com.tchalanet.server.common.types.id.TenantId;

import java.util.UUID;
import java.util.Map;

public record QueueOfflineCancelCommand(TenantId tenantId, UUID deviceId, Map<String,Object> payload) {}

