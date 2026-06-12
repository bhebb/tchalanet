package com.tchalanet.server.platform.audit.internal.archive;

import com.tchalanet.server.platform.archive.api.ArchiveDatasetProvider;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetKey;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetPlan;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportResult;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupResult;
import com.tchalanet.server.platform.archive.api.model.ArchivePeriod;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Archive dataset provider for {@code audit_log}.
 *
 * <p>Retention: P12M. Partition key: {@code occurred_at}.
 * Full export and lookup implementation requires object-storage integration (Phase 5b).
 */
@Component
public class AuditLogArchiveDatasetProvider implements ArchiveDatasetProvider {

  private static final ArchiveDatasetKey KEY =
      ArchiveDatasetKey.of("audit_log", "Audit Log");

  @Override
  public ArchiveDatasetKey key() {
    return KEY;
  }

  @Override
  public ArchiveDatasetPlan plan(ArchivePeriod period, UUID tenantId) {
    // TODO: count audit_log rows WHERE occurred_at in [period.start(), period.end())
    return new ArchiveDatasetPlan(KEY, period, tenantId, 0L, false);
  }

  @Override
  public ArchiveExportResult export(ArchiveExportRequest request) {
    throw new UnsupportedOperationException(
        "audit_log export not yet implemented — requires object-storage integration");
  }

  @Override
  public ArchiveLookupResult lookup(ArchiveLookupRequest request) {
    // audit_log rows are not individually indexed in archive_lookup_index;
    // queries go via table_name + tenant_id + business_date range scan.
    return ArchiveLookupResult.notFound();
  }
}
