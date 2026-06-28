package com.tchalanet.server.core.sales.internal.infra.archive;

import com.tchalanet.server.core.sales.internal.infra.persistence.TicketArchiveJdbcRepository;
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

/** Archive dataset provider for {@code sales_ticket_charge} rows. */
@Component
@RequiredArgsConstructor
@Slf4j
public class SalesTicketChargeArchiveDatasetProvider implements ArchiveDatasetProvider {

  static final int SCHEMA_VERSION = 1;
  static final String TABLE = "sales_ticket_charge";

  private static final ArchiveDatasetKey KEY = ArchiveDatasetKey.of(TABLE, "Sales Ticket Charges");

  private final TicketArchiveJdbcRepository ticketRepo;

  @Override
  public ArchiveDatasetKey key() {
    return KEY;
  }

  @Override
  public ArchiveDatasetPlan plan(ArchivePeriod period, UUID tenantId) {
    Instant from = period.start().atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to = period.end().atStartOfDay(ZoneOffset.UTC).toInstant();
    long count = ticketRepo.countChargesByTicketPeriod(from, to, tenantId);
    return new ArchiveDatasetPlan(KEY, period, tenantId, count, count > 0);
  }

  @Override
  public ArchiveExportResult export(ArchiveExportRequest request) {
    Instant from = request.period().start().atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to = request.period().end().atStartOfDay(ZoneOffset.UTC).toInstant();

    long[] exported = {0};
    ticketRepo.streamChargesByTicketPeriod(from, to, request.tenantId(), row -> {
      request.rowSink().accept(row);
      exported[0]++;
    });

    log.info("sales_ticket_charge export: {} rows period={}/{} tenant={}",
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
    Instant to = period.end().atStartOfDay(ZoneOffset.UTC).toInstant();

    return ticketRepo.findChargeLookupRows(from, to, tenantId).stream()
        .map(r -> new ArchiveLookupEntry(
            TABLE,
            (UUID) r.get("tenant_id"),
            "TICKET",
            (UUID) r.get("id"),
            (String) r.get("public_code"),
            null,
            (Instant) r.get("sold_at"),
            archiveObjectId,
            null,
            null
        ))
        .toList();
  }
}
