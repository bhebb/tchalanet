package com.tchalanet.server.platform.archive.api.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One row to insert into {@code archive_lookup_index}.
 *
 * <p>Providers return these from
 * {@link com.tchalanet.server.platform.archive.api.ArchiveDatasetProvider#generateLookupRows}
 * after the executor has persisted the archive object and obtained its id.
 */
public record ArchiveLookupEntry(
    String tableName,
    UUID tenantId,
    String entityType,
    UUID entityId,
    String publicCode,
    LocalDate businessDate,
    Instant occurredAt,
    UUID archiveObjectId,
    Long objectOffset,
    Long objectLength
) {}
