package com.tchalanet.server.core.offlinesync.application.command.model;

import java.util.UUID;

public record MarkTransactionAsSyncedCommand(UUID tenantId, UUID deviceId, UUID transactionId) {}

