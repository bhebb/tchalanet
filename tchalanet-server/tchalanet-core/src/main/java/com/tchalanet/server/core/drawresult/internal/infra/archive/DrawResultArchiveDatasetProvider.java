package com.tchalanet.server.core.drawresult.internal.infra.archive;

import com.tchalanet.server.core.drawresult.internal.infra.persistence.DrawResultArchiveJdbcRepository;
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

/** Archive dataset provider for global {@code draw_result} rows. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DrawResultArchiveDatasetProvider implements ArchiveDatasetProvider {

  static final int SCHEMA_VERSION = 1;
  static final String TABLE = "draw_result";

  private static final ArchiveDatasetKey KEY = ArchiveDatasetKey.of(TABLE, "Draw Results");

  private final DrawResultArchiveJdbcRepository drawResultRepo;

  @Override
  public ArchiveDatasetKey key() {
    return KEY;
  }

  @Override
  public ArchiveDatasetPlan plan(ArchivePeriod period, UUID tenantId) {
    Instant from = period.start().atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to = period.end().atStartOfDay(ZoneOffset.UTC).toInstant();
    long count = drawResultRepo.countByOccurredPeriod(from, to);
    return new ArchiveDatasetPlan(KEY, period, null, count, count > 0);
  }

  @Override
  public ArchiveExportResult export(ArchiveExportRequest request) {
    Instant from = request.period().start().atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to = request.period().end().atStartOfDay(ZoneOffset.UTC).toInstant();

    long[] exported = {0};
    drawResultRepo.streamByOccurredPeriod(from, to, row -> {
      request.rowSink().accept(row);
      exported[0]++;
    });

    log.info("draw_result export: {} rows period={}/{}",
        exported[0], request.period().start(), request.period().end());
    return new ArchiveExportResult(exported[0], SCHEMA_VERSION);
  }

  @Override
  public ArchiveLookupResult lookup(ArchiveLookupRequest request) {
    return ArchiveLookupResult.notFound();
  }

  @Override
  public List<ArchiveLookupEntry> generateLookupRows(
      ArchivePeriod period, UUID tenantId, UUID archiveObjectId) {

    Instant from = period.start().atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to = period.end().atStartOfDay(ZoneOffset.UTC).toInstant();

    return drawResultRepo.findLookupRows(from, to).stream()
        .map(r -> new ArchiveLookupEntry(
            TABLE,
            null,
            "DRAW_RESULT",
            (UUID) r.get("id"),
            (String) r.get("source_hash"),
            (java.time.LocalDate) r.get("result_date"),
            (Instant) r.get("occurred_at"),
            archiveObjectId,
            null,
            null
        ))
        .toList();
  }
}
