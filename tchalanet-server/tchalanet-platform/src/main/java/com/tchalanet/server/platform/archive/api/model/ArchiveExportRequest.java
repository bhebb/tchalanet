package com.tchalanet.server.platform.archive.api.model;

import java.util.Map;
import java.util.UUID;

/**
 * Parameters for a single dataset export segment.
 *
 * <p>{@code tenantId} is {@code null} for global/platform datasets.
 *
 * <p>Providers call {@link RowSink#accept} once per row. The executor wraps the
 * sink to stream rows through the gzip writer — providers never touch storage directly.
 */
public record ArchiveExportRequest(
    UUID archiveRunId,
    ArchiveDatasetKey datasetKey,
    ArchivePeriod period,
    UUID tenantId,
    int segmentNo,
    RowSink rowSink
) {

  @FunctionalInterface
  public interface RowSink {
    void accept(Map<String, Object> row);
  }
}
