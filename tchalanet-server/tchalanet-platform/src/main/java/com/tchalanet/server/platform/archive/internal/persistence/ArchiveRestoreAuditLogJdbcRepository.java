package com.tchalanet.server.platform.archive.internal.persistence;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Writes rows into {@code archive_restore_audit_log}.
 *
 * <p>The {@code payloadJson} parameter must already be a valid JSON string;
 * serialization is the caller's responsibility.
 */
@Repository
@RequiredArgsConstructor
public class ArchiveRestoreAuditLogJdbcRepository {

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Batch-insert rows into the restore table.
   *
   * @param rows each entry: {tenantId, originalId, occurredAt, payloadJson, schemaVersion}
   */
  public void insertBatch(UUID restoreRunId, UUID archiveObjectId,
      List<RestoreAuditRow> rows) {

    if (rows.isEmpty()) return;
    MapSqlParameterSource[] batch = rows.stream()
        .map(r -> new MapSqlParameterSource()
            .addValue("restoreRunId",    restoreRunId)
            .addValue("tenantId",        r.tenantId())
            .addValue("originalId",      r.originalId())
            .addValue("occurredAt",      r.occurredAt() != null ? Timestamp.from(r.occurredAt()) : null)
            .addValue("archiveObjectId", archiveObjectId)
            .addValue("payload",         r.payloadJson())
            .addValue("schemaVersion",   r.schemaVersion()))
        .toArray(MapSqlParameterSource[]::new);

    jdbc.batchUpdate("""
        INSERT INTO archive_restore_audit_log
          (restore_run_id, tenant_id, original_id, occurred_at,
           archive_object_id, payload, schema_version)
        VALUES
          (:restoreRunId, :tenantId, :originalId, :occurredAt,
           :archiveObjectId, :payload::jsonb, :schemaVersion)
        """, batch);
  }

  public void deleteByRestoreRunId(UUID restoreRunId) {
    jdbc.update(
        "DELETE FROM archive_restore_audit_log WHERE restore_run_id = :id",
        Map.of("id", restoreRunId));
  }

  public record RestoreAuditRow(
      UUID tenantId,
      UUID originalId,
      java.time.Instant occurredAt,
      String payloadJson,
      int schemaVersion
  ) {}
}
