package com.tchalanet.server.platform.archive.api.model;

import java.util.UUID;

/**
 * Estimated plan for archiving one dataset for one period.
 *
 * <p>The archive orchestrator uses this to decide whether to proceed, skip, or
 * warn about unexpectedly large exports before they start.
 */
public record ArchiveDatasetPlan(
    ArchiveDatasetKey datasetKey,
    ArchivePeriod period,
    UUID tenantId,
    long estimatedRowCount,
    boolean archivalNeeded
) {}
