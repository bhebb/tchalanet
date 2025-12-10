package com.tchalanet.server.core.offlinesync.application.command.model;

import java.util.UUID;
import java.util.Map;

public record QueueOfflinePayoutCommand(UUID tenantId, UUID deviceId, Map<String,Object> payload) {}

