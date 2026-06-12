package com.tchalanet.server.platform.archive.api.model;

import java.time.Instant;
import java.util.UUID;

public record ArchiveRunView(
    UUID id,
    String status,
    String strategy,
    String triggerType,
    String idempotencyKey,
    Instant startedAt,
    Instant completedAt,
    String errorMessage
) {}
