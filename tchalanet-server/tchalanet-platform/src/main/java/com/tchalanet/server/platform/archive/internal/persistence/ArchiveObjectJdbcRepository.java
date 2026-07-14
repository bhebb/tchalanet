package com.tchalanet.server.platform.archive.internal.persistence;

import com.tchalanet.server.platform.archive.api.model.ArchiveObjectRowView;
import java.time.LocalDate;
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
public class ArchiveObjectJdbcRepository {

  private static final RowMapper<ArchiveObjectRowView> ROW_MAPPER =
      (rs, rowNum) ->
          new ArchiveObjectRowView(
              rs.getObject("id", UUID.class),
              rs.getObject("archive_run_id", UUID.class),
              rs.getString("table_name"),
              rs.getObject("tenant_id", UUID.class),
              rs.getObject("period_start", LocalDate.class),
              rs.getObject("period_end", LocalDate.class),
              rs.getTimestamp("lower_bound_at") == null
                  ? null
                  : rs.getTimestamp("lower_bound_at").toInstant(),
              rs.getTimestamp("upper_bound_at") == null
                  ? null
                  : rs.getTimestamp("upper_bound_at").toInstant(),
              rs.getInt("segment_no"),
              rs.getString("object_uri"),
              rs.getString("format"),
              rs.getString("compression"),
              rs.getLong("row_count"),
              rs.getLong("byte_size"),
              rs.getString("checksum_sha256"),
              rs.getInt("schema_version"),
              rs.getString("status"),
              rs.getTimestamp("created_at").toInstant());

  private final NamedParameterJdbcTemplate jdbc;

  public UUID insert(
      UUID id,
      UUID archiveRunId,
      String tableName,
      UUID tenantId,
      LocalDate periodStart,
      LocalDate periodEnd,
      int segmentNo,
      String objectUri,
      long rowCount,
      long byteSize,
      String checksumSha256,
      int schemaVersion) {

    jdbc.update(
        """
        INSERT INTO archive_object
          (id, archive_run_id, table_name, tenant_id, period_start, period_end,
           segment_no, object_uri, row_count, byte_size, checksum_sha256,
           schema_version, status)
        VALUES
          (:id, :runId, :table, :tenantId, :pStart, :pEnd,
           :segNo, :uri, :rows, :bytes, :checksum,
           :schema, 'PENDING')
        """,
        new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("runId", archiveRunId)
            .addValue("table", tableName)
            .addValue("tenantId", tenantId)
            .addValue("pStart", periodStart)
            .addValue("pEnd", periodEnd)
            .addValue("segNo", segmentNo)
            .addValue("uri", objectUri)
            .addValue("rows", rowCount)
            .addValue("bytes", byteSize)
            .addValue("checksum", checksumSha256)
            .addValue("schema", schemaVersion));
    return id;
  }

  public void markVerified(UUID id) {
    jdbc.update("UPDATE archive_object SET status = 'VERIFIED' WHERE id = :id", Map.of("id", id));
  }

  public void markInvalid(UUID id) {
    jdbc.update("UPDATE archive_object SET status = 'INVALID' WHERE id = :id", Map.of("id", id));
  }

  public Optional<ArchiveObjectRowView> findById(UUID id) {
    List<ArchiveObjectRowView> rows =
        jdbc.query("SELECT * FROM archive_object WHERE id = :id", Map.of("id", id), ROW_MAPPER);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  public List<ArchiveObjectRowView> findByRunId(UUID runId) {
    return jdbc.query(
        "SELECT * FROM archive_object WHERE archive_run_id = :runId ORDER BY segment_no",
        Map.of("runId", runId),
        ROW_MAPPER);
  }

  public List<ArchiveObjectRowView> listInvalid(int limit) {
    return jdbc.query(
        "SELECT * FROM archive_object WHERE status = 'INVALID' ORDER BY created_at DESC LIMIT :limit",
        Map.of("limit", limit),
        ROW_MAPPER);
  }

  public long countByStatus(String status) {
    Long n =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM archive_object WHERE status = :status",
            Map.of("status", status),
            Long.class);
    return n != null ? n : 0L;
  }
}
