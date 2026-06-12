package com.tchalanet.server.core.payout.internal.infra.archive;

import com.tchalanet.server.platform.archive.api.ArchiveDatasetProvider;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetKey;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetPlan;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportResult;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupEntry;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupResult;
import com.tchalanet.server.platform.archive.api.model.ArchivePeriod;
import com.tchalanet.server.core.payout.internal.infra.persistence.PayoutArchiveJdbcRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Archive dataset provider for {@code payout}.
 *
 * <p>Retention: P24M. Partition key: {@code created_at}.
 *
 * <p>V1: exports payout header rows only. Lookup index has one entry per payout (by id).
 * No public_code on payout — lookup is by entity id only.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PayoutArchiveDatasetProvider implements ArchiveDatasetProvider {

  static final int SCHEMA_VERSION = 1;
  static final String TABLE = "payout";

  private static final ArchiveDatasetKey KEY = ArchiveDatasetKey.of(TABLE, "Payout");

  private final PayoutArchiveJdbcRepository payoutRepo;

  @Override
  public ArchiveDatasetKey key() {
    return KEY;
  }

  @Override
  public ArchiveDatasetPlan plan(ArchivePeriod period, UUID tenantId) {
    Instant from = period.start().atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to   = period.end().atStartOfDay(ZoneOffset.UTC).toInstant();
    long count   = payoutRepo.countByPeriod(from, to, tenantId);
    return new ArchiveDatasetPlan(KEY, period, tenantId, count, count > 0);
  }

  @Override
  public ArchiveExportResult export(ArchiveExportRequest request) {
    Instant from = request.period().start().atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to   = request.period().end().atStartOfDay(ZoneOffset.UTC).toInstant();

    long[] exported = {0};
    payoutRepo.streamByPeriod(from, to, request.tenantId(), row -> {
      request.rowSink().accept(row);
      exported[0]++;
    });

    log.info("payout export: {} rows period={}/{} tenant={}",
        exported[0], request.period().start(), request.period().end(), request.tenantId());
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
    Instant to   = period.end().atStartOfDay(ZoneOffset.UTC).toInstant();

    return payoutRepo.findLookupRows(from, to, tenantId).stream()
        .map(r -> new ArchiveLookupEntry(
            TABLE,
            (UUID) r.get("tenant_id"),
            "PAYOUT",
            (UUID) r.get("id"),
            null,                                    // no public_code on payout
            null,                                    // no business_date on payout
            (Instant) r.get("created_at"),
            archiveObjectId,
            null,
            null
        ))
        .toList();
  }
}
