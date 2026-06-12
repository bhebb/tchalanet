package com.tchalanet.server.platform.audit.internal.archive;

import com.tchalanet.server.platform.archive.api.ArchiveDatasetProvider;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetKey;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetPlan;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportResult;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupEntry;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupResult;
import com.tchalanet.server.platform.archive.api.model.ArchivePeriod;
import com.tchalanet.server.platform.audit.internal.persistence.AuditLogJdbcRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Archive dataset provider for {@code audit_log}.
 *
 * <p>Retention: P12M. Partition key: {@code occurred_at}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogArchiveDatasetProvider implements ArchiveDatasetProvider {

  static final int SCHEMA_VERSION = 1;
  static final String TABLE = "audit_log";

  private static final ArchiveDatasetKey KEY = ArchiveDatasetKey.of(TABLE, "Audit Log");

  private final AuditLogJdbcRepository auditLogRepo;

  @Override
  public ArchiveDatasetKey key() {
    return KEY;
  }

  @Override
  public ArchiveDatasetPlan plan(ArchivePeriod period, UUID tenantId) {
    Instant from = period.start().atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to   = period.end().atStartOfDay(ZoneOffset.UTC).toInstant();
    long count   = auditLogRepo.countByPeriod(from, to, tenantId);
    return new ArchiveDatasetPlan(KEY, period, tenantId, count, count > 0);
  }

  @Override
  public ArchiveExportResult export(ArchiveExportRequest request) {
    Instant from = request.period().start().atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to   = request.period().end().atStartOfDay(ZoneOffset.UTC).toInstant();

    long[] rowsExported = {0};
    auditLogRepo.streamByPeriod(from, to, request.tenantId(), row -> {
      request.rowSink().accept(row);
      rowsExported[0]++;
    });

    log.info("audit_log export: {} rows for period {}/{} tenant={}",
        rowsExported[0], request.period().start(), request.period().end(), request.tenantId());

    return new ArchiveExportResult(rowsExported[0], SCHEMA_VERSION);
  }

  @Override
  public ArchiveLookupResult lookup(ArchiveLookupRequest request) {
    // audit_log uses table+tenant+business_date range scan via archive_lookup_index;
    // single-entity lookup is not supported for audit rows.
    return ArchiveLookupResult.notFound();
  }

  @Override
  public List<ArchiveLookupEntry> generateLookupRows(
      ArchivePeriod period, UUID tenantId, UUID archiveObjectId) {

    // For audit_log: one lookup entry per distinct (tenant_id, entity_type, entity_id, business_date).
    // We generate a coarser-grained index: one entry per (tenant, date) pointing to the object,
    // so archive queries can locate the right object for a date range without scanning all objects.
    // Fine-grained per-entity entries are omitted for V1 (audit volume is too high).
    return auditLogRepo.findDistinctEntityLookupRows(
        period.start().atStartOfDay(ZoneOffset.UTC).toInstant(),
        period.end().atStartOfDay(ZoneOffset.UTC).toInstant(),
        tenantId,
        archiveObjectId
    ).stream()
        .map(r -> new ArchiveLookupEntry(
            TABLE,
            (UUID) r.get("tenant_id"),
            (String) r.get("entity_type"),
            (UUID) r.get("entity_id"),
            null,
            null,
            (Instant) r.get("occurred_at"),
            archiveObjectId,
            null,
            null
        ))
        .toList();
  }
}
