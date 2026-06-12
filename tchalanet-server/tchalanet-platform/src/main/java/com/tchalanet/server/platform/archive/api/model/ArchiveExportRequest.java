package com.tchalanet.server.platform.archive.api.model;

import java.util.UUID;

/**
 * Parameters for a single dataset export segment.
 *
 * <p>{@code tenantId} is {@code null} for global/platform datasets.
 */
public record ArchiveExportRequest(
    UUID archiveRunId,
    ArchiveDatasetKey datasetKey,
    ArchivePeriod period,
    UUID tenantId,
    int segmentNo
) {}
