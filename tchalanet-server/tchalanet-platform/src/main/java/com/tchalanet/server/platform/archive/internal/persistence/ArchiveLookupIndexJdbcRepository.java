package com.tchalanet.server.platform.archive.internal.persistence;

import com.tchalanet.server.platform.archive.api.model.ArchiveLookupEntry;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArchiveLookupIndexJdbcRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public void insertBatch(List<ArchiveLookupEntry> entries) {
    if (entries.isEmpty()) return;
    MapSqlParameterSource[] batch = entries.stream()
        .map(e -> new MapSqlParameterSource()
            .addValue("id",          UUID.randomUUID())
            .addValue("tableName",   e.tableName())
            .addValue("tenantId",    e.tenantId())
            .addValue("entityType",  e.entityType())
            .addValue("entityId",    e.entityId())
            .addValue("publicCode",  e.publicCode())
            .addValue("bizDate",     e.businessDate())
            .addValue("occurredAt",  e.occurredAt() != null ? Timestamp.from(e.occurredAt()) : null)
            .addValue("objectId",    e.archiveObjectId())
            .addValue("offset",      e.objectOffset())
            .addValue("length",      e.objectLength()))
        .toArray(MapSqlParameterSource[]::new);

    jdbc.batchUpdate("""
        INSERT INTO archive_lookup_index
          (id, table_name, tenant_id, entity_type, entity_id, public_code,
           business_date, occurred_at, archive_object_id, object_offset, object_length)
        VALUES
          (:id, :tableName, :tenantId, :entityType, :entityId, :publicCode,
           :bizDate, :occurredAt, :objectId, :offset, :length)
        """, batch);
  }

  /** Find lookup entries for a specific entity. RLS filters tenant visibility. */
  public List<Map<String, Object>> findByEntity(String tableName, String entityType, UUID entityId) {
    return jdbc.queryForList("""
        SELECT * FROM archive_lookup_index
         WHERE table_name   = :table
           AND entity_type  = :entityType
           AND entity_id    = :entityId
         ORDER BY occurred_at DESC
        """,
        new MapSqlParameterSource()
            .addValue("table",      tableName)
            .addValue("entityType", entityType)
            .addValue("entityId",   entityId));
  }

  /** Find lookup entries by public code (e.g., ticket public code). */
  public List<Map<String, Object>> findByPublicCode(String tableName, String publicCode) {
    return jdbc.queryForList("""
        SELECT * FROM archive_lookup_index
         WHERE table_name  = :table
           AND public_code = :code
        """,
        Map.of("table", tableName, "code", publicCode));
  }

  /** Find lookup entries for a table + tenant + date range. */
  public List<Map<String, Object>> findByTenantAndDate(
      String tableName, UUID tenantId, java.time.LocalDate from, java.time.LocalDate to) {
    return jdbc.queryForList("""
        SELECT DISTINCT archive_object_id FROM archive_lookup_index
         WHERE table_name   = :table
           AND tenant_id    = :tenantId
           AND business_date BETWEEN :from AND :to
        """,
        new MapSqlParameterSource()
            .addValue("table",    tableName)
            .addValue("tenantId", tenantId)
            .addValue("from",     from)
            .addValue("to",       to));
  }
}
