package com.tchalanet.server.platform.archive.internal.persistence;

import com.tchalanet.server.platform.archive.api.model.ArchiveRunRowView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArchiveRunJdbcRepository {

  private static final RowMapper<ArchiveRunRowView> ROW_MAPPER =
      (rs, rowNum) ->
          new ArchiveRunRowView(
              rs.getObject("id", UUID.class),
              rs.getString("status"),
              rs.getString("strategy"),
              rs.getString("trigger_type"),
              rs.getString("idempotency_key"),
              rs.getTimestamp("started_at").toInstant(),
              rs.getTimestamp("completed_at") == null
                  ? null
                  : rs.getTimestamp("completed_at").toInstant(),
              rs.getObject("requested_by", UUID.class),
              rs.getString("reason"),
              rs.getString("error_message"),
              rs.getTimestamp("created_at").toInstant());

  private final NamedParameterJdbcTemplate jdbc;

  public UUID insert(
      UUID id,
      String status,
      String strategy,
      String triggerType,
      String idempotencyKey,
      UUID requestedBy,
      String reason) {

    jdbc.update(
        """
        INSERT INTO archive_run
          (id, status, strategy, trigger_type, idempotency_key, started_at, requested_by, reason)
        VALUES
          (:id, :status, :strategy, :triggerType, :idemKey, now(), :requestedBy, :reason)
        """,
        new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("status", status)
            .addValue("strategy", strategy)
            .addValue("triggerType", triggerType)
            .addValue("idemKey", idempotencyKey)
            .addValue("requestedBy", requestedBy)
            .addValue("reason", reason));
    return id;
  }

  public Optional<ArchiveRunRowView> findByIdempotencyKey(String key) {
    List<ArchiveRunRowView> rows =
        jdbc.query(
            "SELECT * FROM archive_run WHERE idempotency_key = :key",
            Map.of("key", key),
            ROW_MAPPER);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  public void updateStatus(UUID id, String status) {
    jdbc.update(
        "UPDATE archive_run SET status = :status WHERE id = :id",
        Map.of("status", status, "id", id));
  }

  public void complete(UUID id) {
    jdbc.update(
        "UPDATE archive_run SET status = 'COMPLETED', completed_at = now() WHERE id = :id",
        Map.of("id", id));
  }

  public void fail(UUID id, String errorMessage) {
    jdbc.update(
        "UPDATE archive_run SET status = 'FAILED', error_message = :msg WHERE id = :id",
        new MapSqlParameterSource().addValue("id", id).addValue("msg", errorMessage));
  }

  public List<ArchiveRunRowView> listRecent(int limit) {
    return jdbc.query(
        "SELECT * FROM archive_run ORDER BY started_at DESC LIMIT :limit",
        Map.of("limit", limit),
        ROW_MAPPER);
  }

  public List<ArchiveRunRowView> listFailed(int limit) {
    return jdbc.query(
        "SELECT * FROM archive_run WHERE status = 'FAILED' ORDER BY started_at DESC LIMIT :limit",
        Map.of("limit", limit),
        ROW_MAPPER);
  }

  public long countByStatus(String status) {
    Long n =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM archive_run WHERE status = :status",
            Map.of("status", status),
            Long.class);
    return n != null ? n : 0L;
  }
}
