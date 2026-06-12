package com.tchalanet.server.platform.archive.internal.service;

import com.tchalanet.server.platform.archive.internal.config.ArchiveProperties;
import com.tchalanet.server.platform.archive.internal.io.JsonlGzReader;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveLookupIndexJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveObjectJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRestoreAuditLogJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRestoreAuditLogJdbcRepository.RestoreAuditRow;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRestoreRunJdbcRepository;
import com.tchalanet.server.platform.archive.internal.storage.ArchiveStoragePort;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Platform-only restore service. Copies archived rows into TTL-bounded restore tables
 * so they can be queried for investigation without requiring a full re-import.
 *
 * <p>All operations require SUPER_ADMIN authority and a mandatory reason (enforced at
 * the controller layer via {@link @PreAuthorize} + {@link @Valid}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveRestoreService {

  private final ArchiveProperties props;
  private final ArchiveRestoreRunJdbcRepository restoreRunRepo;
  private final ArchiveRestoreAuditLogJdbcRepository restoreAuditRepo;
  private final ArchiveLookupIndexJdbcRepository lookupRepo;
  private final ArchiveObjectJdbcRepository objectRepo;
  private final ArchiveStoragePort storage;
  private final ObjectMapper objectMapper;

  // ── Restore execution ───────────────────────────────────────────────────────

  /**
   * Restore archived audit_log rows for the given entity into the temporary restore table.
   *
   * @param tenantId    (nullable) filter to one tenant, or null for global
   * @param entityType  entity type filter
   * @param entityId    entity ID filter
   * @param from        start of date range (inclusive)
   * @param to          end of date range (exclusive)
   * @param requestedBy SUPER_ADMIN user ID
   * @param reason      mandatory justification (min 10 chars, validated at controller)
   * @return restore run ID
   */
  public UUID restoreAuditLog(UUID tenantId, String entityType, UUID entityId,
      LocalDate from, LocalDate to, UUID requestedBy, String reason) {

    checkActiveRestoreLimit();

    Instant expiresAt = Instant.now().plus(props.restore().tempTtl());
    UUID restoreRunId = restoreRunRepo.insert(requestedBy, reason, expiresAt, "[]");

    List<Map<String, Object>> lookupEntries =
        lookupRepo.findByEntity("audit_log", entityType, entityId);

    if (lookupEntries.isEmpty()) {
      log.info("archive restore: no lookup entries for entity={}:{}", entityType, entityId);
      return restoreRunId;
    }

    String entityIdStr = entityId.toString();
    JsonlGzReader reader = new JsonlGzReader(objectMapper);
    long totalRestored = 0;

    for (Map<String, Object> entry : lookupEntries) {
      UUID objectId = (UUID) entry.get("archive_object_id");
      Optional<Map<String, Object>> objMeta = objectRepo.findById(objectId);
      if (objMeta.isEmpty()) {
        log.warn("archive restore: missing archive_object id={}", objectId);
        continue;
      }

      String uri = (String) objMeta.get().get("object_uri");
      int schemaVersion = ((Number) objMeta.get().getOrDefault("schema_version", 1)).intValue();

      if (!storage.exists(uri)) {
        log.warn("archive restore: object not in storage uri={}", uri);
        continue;
      }

      List<RestoreAuditRow> batch = new ArrayList<>();
      try (InputStream in = storage.openRead(uri)) {
        List<Map<String, Object>> rows = reader.readMatching(in, row ->
            entityType.equals(row.get("entity_type"))
                && entityIdStr.equals(String.valueOf(row.get("entity_id"))));

        for (Map<String, Object> row : rows) {
          batch.add(new RestoreAuditRow(
              toUuid(row.get("tenant_id")),
              toUuid(row.get("id")),
              toInstant(row.get("occurred_at")),
              serialize(row),
              schemaVersion
          ));
        }
      } catch (IOException ex) {
        throw new UncheckedIOException("Failed to read archive for restore: " + uri, ex);
      }

      if (!batch.isEmpty()) {
        restoreAuditRepo.insertBatch(restoreRunId, objectId, batch);
        restoreRunRepo.incrementRowCount(restoreRunId, batch.size());
        totalRestored += batch.size();
      }
    }

    log.info("archive restore: restoreRun={} entity={}:{} rows={} expiresAt={}",
        restoreRunId, entityType, entityId, totalRestored, expiresAt);
    return restoreRunId;
  }

  // ── Cleanup ─────────────────────────────────────────────────────────────────

  /**
   * Delete rows from expired restore runs and mark runs as CLEANED.
   * Safe to call repeatedly — idempotent on already-cleaned runs.
   */
  public int cleanupExpired() {
    List<Map<String, Object>> expired = restoreRunRepo.findExpiredActive();
    int cleaned = 0;
    for (Map<String, Object> run : expired) {
      UUID runId = (UUID) run.get("id");
      restoreAuditRepo.deleteByRestoreRunId(runId);
      restoreRunRepo.markCleaned(runId);
      cleaned++;
      log.info("archive restore cleanup: run={} cleaned", runId);
    }
    if (cleaned > 0) {
      log.info("archive restore cleanup: {} expired runs cleaned", cleaned);
    }
    return cleaned;
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  private void checkActiveRestoreLimit() {
    int active = restoreRunRepo.countActive();
    int max = props.restore().maxActiveRestoreRuns();
    if (active >= max) {
      throw new IllegalStateException(
          "Max active restore runs (%d) reached — wait for existing runs to expire".formatted(max));
    }
  }

  private String serialize(Map<String, Object> row) {
    try {
      return objectMapper.writeValueAsString(row);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to serialize restore row", e);
    }
  }

  private static UUID toUuid(Object val) {
    if (val == null) return null;
    if (val instanceof UUID u) return u;
    try { return UUID.fromString(val.toString()); } catch (IllegalArgumentException e) { return null; }
  }

  private static Instant toInstant(Object val) {
    if (val == null) return null;
    if (val instanceof Instant i) return i;
    if (val instanceof java.sql.Timestamp ts) return ts.toInstant();
    return null;
  }
}
