package com.tchalanet.server.platform.archive.api.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Query parameters for an archive lookup.
 *
 * <p>At least one of {@code entityId} or {@code publicCode} should be non-null.
 */
public record ArchiveLookupRequest(
    ArchiveDatasetKey datasetKey,
    UUID tenantId,
    UUID entityId,
    String publicCode,
    LocalDate businessDate
) {}
