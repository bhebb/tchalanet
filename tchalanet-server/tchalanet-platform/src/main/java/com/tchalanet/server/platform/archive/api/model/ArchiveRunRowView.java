package com.tchalanet.server.platform.archive.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * One {@code archive_run} row, verbatim — same field set/JSON keys as the previous {@code SELECT *}
 * raw map so existing consumers see an unchanged contract.
 */
public record ArchiveRunRowView(
    UUID id,
    String status,
    String strategy,
    @JsonProperty("trigger_type") String triggerType,
    @JsonProperty("idempotency_key") String idempotencyKey,
    @JsonProperty("started_at") Instant startedAt,
    @JsonProperty("completed_at") Instant completedAt,
    @JsonProperty("requested_by") UUID requestedBy,
    String reason,
    @JsonProperty("error_message") String errorMessage,
    @JsonProperty("created_at") Instant createdAt) {}
