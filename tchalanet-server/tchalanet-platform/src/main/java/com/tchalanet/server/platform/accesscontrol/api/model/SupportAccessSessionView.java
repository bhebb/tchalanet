package com.tchalanet.server.platform.accesscontrol.api.model;

import java.time.Instant;
import java.util.UUID;

public record SupportAccessSessionView(
    UUID sessionId,
    UUID tenantId,
    String tenantCode,
    String tenantName,
    Instant startedAt,
    Instant expiresAt,
    String actorRole,
    SupportAccessMode mode,
    boolean sensitiveDataMasked) {}
