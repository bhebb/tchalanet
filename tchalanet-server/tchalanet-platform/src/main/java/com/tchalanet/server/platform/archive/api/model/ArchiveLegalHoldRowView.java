package com.tchalanet.server.platform.archive.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One {@code archive_legal_hold} row, verbatim — same field set/JSON keys as the previous
 * {@code SELECT *} raw map so existing consumers see an unchanged contract.
 */
public record ArchiveLegalHoldRowView(
    UUID id,
    @JsonProperty("tenant_id") UUID tenantId,
    @JsonProperty("dataset_code") String datasetCode,
    @JsonProperty("entity_type") String entityType,
    @JsonProperty("entity_id") String entityId,
    @JsonProperty("period_start") LocalDate periodStart,
    @JsonProperty("period_end") LocalDate periodEnd,
    String reason,
    String status,
    @JsonProperty("created_by_actor_id") UUID createdByActorId,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("released_by_actor_id") UUID releasedByActorId,
    @JsonProperty("released_at") Instant releasedAt,
    @JsonProperty("release_reason") String releaseReason
) {}
