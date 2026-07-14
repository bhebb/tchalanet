package com.tchalanet.server.platform.archive.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One {@code archive_object} row, verbatim — same field set/JSON keys as the previous {@code SELECT
 * *} raw map so existing consumers see an unchanged contract.
 */
public record ArchiveObjectRowView(
    UUID id,
    @JsonProperty("archive_run_id") UUID archiveRunId,
    @JsonProperty("table_name") String tableName,
    @JsonProperty("tenant_id") UUID tenantId,
    @JsonProperty("period_start") LocalDate periodStart,
    @JsonProperty("period_end") LocalDate periodEnd,
    @JsonProperty("lower_bound_at") Instant lowerBoundAt,
    @JsonProperty("upper_bound_at") Instant upperBoundAt,
    @JsonProperty("segment_no") int segmentNo,
    @JsonProperty("object_uri") String objectUri,
    String format,
    String compression,
    @JsonProperty("row_count") long rowCount,
    @JsonProperty("byte_size") long byteSize,
    @JsonProperty("checksum_sha256") String checksumSha256,
    @JsonProperty("schema_version") int schemaVersion,
    String status,
    @JsonProperty("created_at") Instant createdAt) {}
