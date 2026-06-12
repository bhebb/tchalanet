package com.tchalanet.server.platform.archive.internal.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArchiveObjectJdbcRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public UUID insert(UUID id, UUID archiveRunId, String tableName, UUID tenantId,
      LocalDate periodStart, LocalDate periodEnd, int segmentNo,
      String objectUri, long rowCount, long byteSize,
      String checksumSha256, int schemaVersion) {

    jdbc.update("""
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
            .addValue("id",       id)
            .addValue("runId",    archiveRunId)
            .addValue("table",    tableName)
            .addValue("tenantId", tenantId)
            .addValue("pStart",   periodStart)
            .addValue("pEnd",     periodEnd)
            .addValue("segNo",    segmentNo)
            .addValue("uri",      objectUri)
            .addValue("rows",     rowCount)
            .addValue("bytes",    byteSize)
            .addValue("checksum", checksumSha256)
            .addValue("schema",   schemaVersion));
    return id;
  }

  public void markVerified(UUID id) {
    jdbc.update(
        "UPDATE archive_object SET status = 'VERIFIED' WHERE id = :id",
        Map.of("id", id));
  }

  public void markInvalid(UUID id) {
    jdbc.update(
        "UPDATE archive_object SET status = 'INVALID' WHERE id = :id",
        Map.of("id", id));
  }

  public Optional<Map<String, Object>> findById(UUID id) {
    List<Map<String, Object>> rows = jdbc.queryForList(
        "SELECT * FROM archive_object WHERE id = :id",
        Map.of("id", id));
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  public List<Map<String, Object>> findByRunId(UUID runId) {
    return jdbc.queryForList(
        "SELECT * FROM archive_object WHERE archive_run_id = :runId ORDER BY segment_no",
        Map.of("runId", runId));
  }
}
