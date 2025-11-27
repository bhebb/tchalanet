package com.tchalanet.server.offlinesync.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OfflineTicket(
    UUID id, UUID tenantId, UUID terminalId, String payloadJson, Instant receivedAt) {}
