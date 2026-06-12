package com.tchalanet.server.core.sales.internal.infra.archive;

import com.tchalanet.server.platform.archive.api.ArchiveDatasetProvider;
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
import org.springframework.stereotype.Component;

/**
 * Archive dataset provider for {@code sales_ticket} (and its child tables).
 *
 * <p>Retention: P12M. Partition key: {@code sold_at}.
 * Child tables {@code sales_ticket_line} and {@code sales_ticket_charge} are exported
 * as part of this provider's segment (denormalized into the ticket JSON).
 * Full export and lookup implementation requires object-storage integration (Phase 5b).
 */
@Component
public class SalesTicketArchiveDatasetProvider implements ArchiveDatasetProvider {

  private static final ArchiveDatasetKey KEY =
      ArchiveDatasetKey.of("sales_ticket", "Sales Ticket");

  @Override
  public ArchiveDatasetKey key() {
    return KEY;
  }

  @Override
  public ArchiveDatasetPlan plan(ArchivePeriod period, UUID tenantId) {
    // TODO: count sales_ticket rows WHERE sold_at in [period.start(), period.end())
    //       AND tenant_id = tenantId (when non-null)
    return new ArchiveDatasetPlan(KEY, period, tenantId, 0L, false);
  }

  @Override
  public ArchiveExportResult export(ArchiveExportRequest request) {
    throw new UnsupportedOperationException(
        "sales_ticket export not yet implemented — requires object-storage integration");
  }

  @Override
  public ArchiveLookupResult lookup(ArchiveLookupRequest request) {
    // Individual ticket lookup via archive_lookup_index -> object fetch
    // TODO: query archive_lookup_index WHERE entity_type='TICKET' AND entity_id=request.entityId()
    //       OR public_code=request.publicCode(), then fetch + decompress from object storage
    return ArchiveLookupResult.notFound();
  }

  @Override
  public List<ArchiveLookupEntry> generateLookupRows(
      ArchivePeriod period, UUID tenantId, UUID archiveObjectId) {
    // TODO: query sold tickets in period to generate per-ticket lookup entries
    return List.of();
  }
}
