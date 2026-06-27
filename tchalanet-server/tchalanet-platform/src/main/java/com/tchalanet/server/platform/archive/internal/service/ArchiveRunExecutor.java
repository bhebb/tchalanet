package com.tchalanet.server.platform.archive.internal.service;

import com.tchalanet.server.platform.archive.api.ArchiveDatasetProvider;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetPlan;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportResult;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupEntry;
import com.tchalanet.server.platform.archive.api.model.ArchivePeriod;
import com.tchalanet.server.platform.archive.api.model.ArchiveRunView;
import com.tchalanet.server.platform.archive.api.model.TriggerArchiveRunRequest;
import com.tchalanet.server.platform.archive.internal.io.JsonlGzWriter;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveLookupIndexJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveObjectJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRunJdbcRepository;
import com.tchalanet.server.platform.archive.internal.service.ArchiveRunGuard.Decision;
import com.tchalanet.server.platform.archive.internal.storage.ArchiveStoragePort;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Orchestrates a full archive run across all registered {@link ArchiveDatasetProvider}s.
 *
 * <p>For each provider, the executor:
 * <ol>
 *   <li>Calls {@code plan()} to check whether archival is needed for the period.</li>
 *   <li>Opens a write stream to object storage and wraps it with {@link JsonlGzWriter}.</li>
 *   <li>Calls {@code export()} with a {@code RowSink} that streams rows into the writer.</li>
 *   <li>Persists an {@code archive_object} record with checksum and row count.</li>
 *   <li>Calls {@code generateLookupRows()} and bulk-inserts into {@code archive_lookup_index}.</li>
 *   <li>Marks the object {@code VERIFIED}.</li>
 * </ol>
 *
 * <p>Idempotency is enforced by {@link ArchiveRunGuard} at the run level.
 * V1 runs all datasets globally (tenantId = null). Per-tenant iteration is V2.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArchiveRunExecutor {

  private final List<ArchiveDatasetProvider> providers;
  private final ArchiveRunGuard guard;
  private final ArchiveRunJdbcRepository runRepo;
  private final ArchiveObjectJdbcRepository objectRepo;
  private final ArchiveLookupIndexJdbcRepository lookupRepo;
  private final ArchiveStoragePort storage;
  private final ObjectMapper objectMapper;
  private final ArchiveMetrics metrics;

  /** Execute a full archive run for the given period. Returns the resulting run view. */
  public ArchiveRunView execute(TriggerArchiveRunRequest request, UUID requestedBy) {
    String idemKey = ArchiveIdempotencyKeyBuilder.forRun(
        request.periodStart(), request.periodEnd());

    ArchiveRunGuard.GuardResult guardResult = guard.beginOrResume(
        idemKey, request.strategy(), "MANUAL", requestedBy, request.reason());

    if (guardResult.decision() == Decision.ALREADY_COMPLETED) {
      return loadRunView(guardResult.runId());
    }

    UUID runId = guardResult.runId();
    ArchivePeriod period = new ArchivePeriod(request.periodStart(), request.periodEnd());
    Instant runStart = Instant.now();

    try {
      for (ArchiveDatasetProvider provider : providers) {
        executeDataset(runId, provider, period);
      }
      guard.complete(runId);
      metrics.recordRunCompleted(Duration.between(runStart, Instant.now()));
    } catch (Exception ex) {
      log.error("archive run={} failed after provider loop: {}", runId, ex.getMessage(), ex);
      guard.fail(runId, truncate(ex.getMessage(), 1000));
      metrics.recordRunFailed();
      throw ex;
    }

    return loadRunView(runId);
  }

  // ── Per-dataset execution ───────────────────────────────────────────────────

  private void executeDataset(UUID runId, ArchiveDatasetProvider provider, ArchivePeriod period) {
    // V1: global run — tenantId is null (all tenants within the dataset)
    UUID tenantId = null;
    String tableName = provider.key().tableName();

    ArchiveDatasetPlan plan = provider.plan(period, tenantId);
    if (!plan.archivalNeeded()) {
      log.info("archive: dataset={} period={}/{} — no rows, skipping", tableName, period.start(), period.end());
      return;
    }

    String segmentId = UUID.randomUUID().toString();
    String uri = storage.buildUri(tableName, "global",
        period.start().getYear(), period.start().getMonthValue(), segmentId);

    UUID objectId = UUID.randomUUID();
    StoredArchiveObject result = exportToStorage(provider, period, tenantId, runId, uri);

    objectRepo.insert(objectId, runId, tableName, tenantId,
        period.start(), period.end(), 0,
        uri, result.rowsExported(), result.byteSize(),
        result.checksumSha256(), result.schemaVersion());

    try {
      verifyArchiveObject(plan, result, uri);
    } catch (RuntimeException ex) {
      objectRepo.markInvalid(objectId);
      metrics.recordObjectReadError(tableName);
      throw ex;
    }

    List<ArchiveLookupEntry> lookupEntries =
        provider.generateLookupRows(period, tenantId, objectId);
    if (!lookupEntries.isEmpty()) {
      lookupRepo.insertBatch(lookupEntries);
    }

    objectRepo.markVerified(objectId);
    metrics.recordRowsExported(tableName, result.rowsExported());
    log.info("archive: dataset={} objectId={} period={}/{} rows={} uri={}",
        tableName, objectId, period.start(), period.end(), result.rowsExported(), uri);
  }

  private StoredArchiveObject exportToStorage(ArchiveDatasetProvider provider,
      ArchivePeriod period, UUID tenantId, UUID runId, String uri) {

    OutputStream out = storage.openWrite(uri);
    JsonlGzWriter writer = new JsonlGzWriter(out, objectMapper);
    ArchiveExportResult providerResult;
    boolean exportSucceeded = false;

    try {
      ArchiveExportRequest exportReq = new ArchiveExportRequest(
          runId, provider.key(), period, tenantId, 0, writer::write);
      providerResult = provider.export(exportReq);
      exportSucceeded = true;
    } finally {
      try {
        writer.close();
      } catch (IOException e) {
        if (exportSucceeded) {
          throw new UncheckedIOException("Failed to close archive writer for " + uri, e);
        }
      }
    }

    return new StoredArchiveObject(
        providerResult.rowsExported(),
        providerResult.schemaVersion(),
        writer.checksumSha256(),
        writer.compressedBytes(),
        writer.rowsWritten(),
        storage.size(uri));
  }

  private void verifyArchiveObject(ArchiveDatasetPlan plan, StoredArchiveObject result, String uri) {
    if (!storage.exists(uri)) {
      throw new IllegalStateException("archive object missing after export: " + uri);
    }
    if (result.rowsExported() != result.rowsWritten()) {
      throw new IllegalStateException("archive row-count mismatch for %s: provider=%d writer=%d"
          .formatted(uri, result.rowsExported(), result.rowsWritten()));
    }
    if (plan.estimatedRowCount() != result.rowsExported()) {
      throw new IllegalStateException("archive plan/export row-count mismatch for %s: plan=%d exported=%d"
          .formatted(uri, plan.estimatedRowCount(), result.rowsExported()));
    }
    if (result.byteSize() <= 0) {
      throw new IllegalStateException("archive object has invalid byte size for " + uri);
    }
    if (result.storageByteSize() != result.byteSize()) {
      throw new IllegalStateException("archive byte-size mismatch for %s: writer=%d storage=%d"
          .formatted(uri, result.byteSize(), result.storageByteSize()));
    }
    if (result.checksumSha256() == null || result.checksumSha256().isBlank()) {
      throw new IllegalStateException("archive checksum missing for " + uri);
    }
  }

  // ── Run view helpers ────────────────────────────────────────────────────────

  private ArchiveRunView loadRunView(UUID runId) {
    return runRepo.listRecent(200).stream()
        .filter(r -> runId.equals(r.get("id")))
        .findFirst()
        .map(this::toRunView)
        .orElseThrow(() -> new IllegalStateException("archive_run not found after execution: " + runId));
  }

  private ArchiveRunView toRunView(Map<String, Object> row) {
    return new ArchiveRunView(
        (UUID) row.get("id"),
        (String) row.get("status"),
        (String) row.get("strategy"),
        (String) row.get("trigger_type"),
        (String) row.get("idempotency_key"),
        toInstant(row.get("started_at")),
        toInstant(row.get("completed_at")),
        (String) row.get("error_message")
    );
  }

  private static java.time.Instant toInstant(Object val) {
    if (val == null) return null;
    if (val instanceof Timestamp ts) return ts.toInstant();
    if (val instanceof java.time.Instant i) return i;
    return null;
  }

  private static String truncate(String s, int max) {
    if (s == null) return null;
    return s.length() <= max ? s : s.substring(0, max);
  }

  private record StoredArchiveObject(
      long rowsExported,
      int schemaVersion,
      String checksumSha256,
      long byteSize,
      long rowsWritten,
      long storageByteSize
  ) {}
}
