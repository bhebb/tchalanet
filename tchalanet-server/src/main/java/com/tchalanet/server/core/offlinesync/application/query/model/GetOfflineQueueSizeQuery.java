package com.tchalanet.server.core.offlinesync.application.query.model;

import java.util.UUID;

public record GetOfflineQueueSizeQuery(UUID tenantId, UUID deviceId) {}

