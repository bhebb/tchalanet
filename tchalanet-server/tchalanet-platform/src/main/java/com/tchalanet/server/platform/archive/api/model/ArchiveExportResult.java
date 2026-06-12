package com.tchalanet.server.platform.archive.api.model;

/**
 * Outcome of a single dataset export segment.
 *
 * <p>{@code objectUri} follows the convention:
 * {@code archive/{env}/{table}/{tenant_or_global}/{yyyy}/{mm}/{segment_id}.jsonl.gz}.
 */
public record ArchiveExportResult(
    long rowsExported,
    String objectUri,
    long compressedBytes,
    String checksumSha256,
    int schemaVersion
) {}
