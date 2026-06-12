package com.tchalanet.server.platform.notification.internal.archive;

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
 * Archive dataset provider for {@code notification_delivery}.
 *
 * <p>Retention: P6M. Partition key: {@code created_at}.
 * Full export implementation requires object-storage integration (Phase 5b).
 */
@Component
public class NotificationArchiveDatasetProvider implements ArchiveDatasetProvider {

  private static final ArchiveDatasetKey KEY =
      ArchiveDatasetKey.of("notification_delivery", "Notification Delivery");

  @Override
  public ArchiveDatasetKey key() {
    return KEY;
  }

  @Override
  public ArchiveDatasetPlan plan(ArchivePeriod period, UUID tenantId) {
    // TODO: count notification_delivery rows WHERE created_at in [period.start(), period.end())
    return new ArchiveDatasetPlan(KEY, period, tenantId, 0L, false);
  }

  @Override
  public ArchiveExportResult export(ArchiveExportRequest request) {
    throw new UnsupportedOperationException(
        "notification_delivery export not yet implemented — requires object-storage integration");
  }

  @Override
  public ArchiveLookupResult lookup(ArchiveLookupRequest request) {
    return ArchiveLookupResult.notFound();
  }
}
