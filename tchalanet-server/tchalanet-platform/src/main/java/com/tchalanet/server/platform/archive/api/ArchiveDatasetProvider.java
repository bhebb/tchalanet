package com.tchalanet.server.platform.archive.api;

import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetKey;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetPlan;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportResult;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupEntry;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupResult;
import com.tchalanet.server.platform.archive.api.model.ArchivePeriod;
import java.util.List;
import java.util.UUID;

/**
 * Contract that owning modules implement to participate in the archive lifecycle.
 *
 * <p>Architecture rules:
 * <ul>
 *   <li>Implementations live in the <em>owning</em> module (e.g., {@code core.sales},
 *       {@code platform.audit}), never in {@code platform.archive.internal}.</li>
 *   <li>{@code platform.archive} orchestrates providers discovered via Spring injection;
 *       it only sees this interface, never the implementation class.</li>
 *   <li>Implementations may use their own internal repositories — that is safe because
 *       they reside in their own module and are not imported by {@code platform.archive}.</li>
 * </ul>
 */
public interface ArchiveDatasetProvider {

  /** Stable identity of the dataset this provider manages. */
  ArchiveDatasetKey key();

  /**
   * Estimate how many rows would be exported for {@code period} and {@code tenantId}.
   *
   * <p>{@code tenantId} is {@code null} for global/platform datasets.
   * The orchestrator uses the plan to decide whether to proceed, skip or warn.
   */
  ArchiveDatasetPlan plan(ArchivePeriod period, UUID tenantId);

  /**
   * Export one segment of data for the given period to object storage.
   *
   * <p>Must be idempotent: if the object already exists for this runId+segmentNo,
   * return the existing result without re-writing.
   */
  ArchiveExportResult export(ArchiveExportRequest request);

  /**
   * Locate and retrieve archived rows matching the lookup request.
   *
   * <p>Returns {@link ArchiveLookupResult#notFound()} when no archive record matches.
   * Never throws for missing data — callers interpret {@code found == false}.
   */
  ArchiveLookupResult lookup(ArchiveLookupRequest request);

  /**
   * Return lookup-index rows to insert after the archive object is persisted.
   *
   * <p>Called by the executor after saving the archive object to DB. The provider
   * knows which fields are meaningful for lookup (e.g., entityId, publicCode,
   * businessDate). The executor inserts the returned entries into
   * {@code archive_lookup_index}.
   *
   * <p>Return an empty list if this dataset does not support per-entity lookup.
   */
  List<ArchiveLookupEntry> generateLookupRows(ArchivePeriod period, UUID tenantId, UUID archiveObjectId);
}
