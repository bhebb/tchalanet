package com.tchalanet.server.core.offlinesync.application.command.model;

import java.util.UUID;

public record RetryFailedSyncCommand(UUID tenantId, UUID deviceId) {}

