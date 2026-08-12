package com.tchalanet.server.core.limitpolicy.internal.infra.archive;

import com.tchalanet.server.platform.archive.api.ArchiveDatasetProvider;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetKey;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetPlan;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportResult;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupEntry;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupResult;
import com.tchalanet.server.platform.archive.api.model.ArchivePeriod;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Archive dataset provider for {@code draw_exposure} rows.
 *
 * <p>Archives by draw lifecycle: rows whose draw has reached ARCHIVED status and whose
 * {@code draw.scheduled_at} falls before the period end are eligible. Runtime limitpolicy queries
 * always filter by a specific {@code draw_id} (active draw) — they never scan archived exposure
 * rows, so hard-delete is safe.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DrawExposureArchiveDatasetProvider implements ArchiveDatasetProvider {

  static final int SCHEMA_VERSION = 1;

  private static final ArchiveDatasetKey KEY =
      ArchiveDatasetKey.of("draw_exposure", "Draw Exposure");

  private final DrawExposureArchiveJdbcRepository repo;

  @Override
  public ArchiveDatasetKey key() {
    return KEY;
  }

  @Override
  public ArchiveDatasetPlan plan(ArchivePeriod period, UUID tenantId) {
    Instant periodEnd = period.end().atStartOfDay(ZoneOffset.UTC).toInstant();
    long count = repo.countByArchivedDraw(periodEnd, tenantId);
    return new ArchiveDatasetPlan(KEY, period, tenantId, count, count > 0);
  }

  @Override
  public ArchiveExportResult export(ArchiveExportRequest request) {
    Instant periodEnd = request.period().end().atStartOfDay(ZoneOffset.UTC).toInstant();

    long exported =
        repo.streamAndDelete(periodEnd, request.tenantId(), row -> request.rowSink().accept(row));

    log.info(
        "draw_exposure archive: {} rows hard-deleted for archived draws before={} tenant={}",
        exported,
        periodEnd,
        request.tenantId());

    return new ArchiveExportResult(exported, SCHEMA_VERSION);
  }

  @Override
  public ArchiveLookupResult lookup(ArchiveLookupRequest request) {
    return ArchiveLookupResult.notFound();
  }

  @Override
  public List<ArchiveLookupEntry> generateLookupRows(
      ArchivePeriod period, UUID tenantId, UUID archiveObjectId) {
    return List.of();
  }
}
