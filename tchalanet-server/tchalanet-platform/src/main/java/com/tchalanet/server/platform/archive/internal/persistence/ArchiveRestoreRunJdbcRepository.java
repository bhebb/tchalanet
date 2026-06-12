package com.tchalanet.server.platform.archive.internal.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArchiveRestoreRunJdbcRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public UUID insert(UUID requestedBy, String reason, Instant expiresAt, String archiveRunIds) {
    UUID id = UUID.randomUUID();
    jdbc.update("""
        INSERT INTO archive_restore_run
          (id, requested_by, reason, status, row_count, archive_run_ids, expires_at)
        VALUES
          (:id, :requestedBy, :reason, 'ACTIVE', 0, :archiveRunIds::jsonb, :expiresAt)
        """,
        new MapSqlParameterSource()
            .addValue("id",            id)
            .addValue("requestedBy",   requestedBy)
            .addValue("reason",        reason)
            .addValue("archiveRunIds", archiveRunIds)
            .addValue("expiresAt",     Timestamp.from(expiresAt)));
    return id;
  }

  public void incrementRowCount(UUID id, long delta) {
    jdbc.update(
        "UPDATE archive_restore_run SET row_count = row_count + :delta WHERE id = :id",
        Map.of("id", id, "delta", delta));
  }

  public void markExpired(UUID id) {
    jdbc.update(
        "UPDATE archive_restore_run SET status = 'EXPIRED' WHERE id = :id",
        Map.of("id", id));
  }

  public void markCleaned(UUID id) {
    jdbc.update(
        "UPDATE archive_restore_run SET status = 'CLEANED' WHERE id = :id",
        Map.of("id", id));
  }

  public int countActive() {
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM archive_restore_run WHERE status = 'ACTIVE'",
        Map.of(), Integer.class);
    return count != null ? count : 0;
  }

  /** Return ACTIVE runs whose expires_at has passed. */
  public List<Map<String, Object>> findExpiredActive() {
    return jdbc.queryForList("""
        SELECT id FROM archive_restore_run
         WHERE status = 'ACTIVE' AND expires_at < now()
        """, Map.of());
  }
}
