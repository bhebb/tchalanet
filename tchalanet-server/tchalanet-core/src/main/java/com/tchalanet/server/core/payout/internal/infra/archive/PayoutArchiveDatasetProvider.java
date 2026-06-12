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
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Archive dataset provider for {@code payout}.
 *
 * <p>Retention: P24M. Partition key: {@code created_at}.
 * Full export and lookup implementation requires object-storage integration (Phase 5b).
 */
@Component
public class PayoutArchiveDatasetProvider implements ArchiveDatasetProvider {

  private static final ArchiveDatasetKey KEY =
      ArchiveDatasetKey.of("payout", "Payout");

  @Override
  public ArchiveDatasetKey key() {
    return KEY;
  }

  @Override
  public ArchiveDatasetPlan plan(ArchivePeriod period, UUID tenantId) {
    // TODO: count payout rows WHERE created_at in [period.start(), period.end())
    return new ArchiveDatasetPlan(KEY, period, tenantId, 0L, false);
  }

  @Override
  public ArchiveExportResult export(ArchiveExportRequest request) {
    throw new UnsupportedOperationException(
        "payout export not yet implemented — requires object-storage integration");
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
