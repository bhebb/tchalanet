package com.tchalanet.server.platform.archive.internal.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArchiveLegalHoldJdbcRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public boolean hasActiveHoldForPeriod(String datasetCode, LocalDate periodStart, LocalDate periodEnd) {
    Integer count = jdbc.queryForObject("""
        SELECT COUNT(*)
          FROM archive_legal_hold
         WHERE status = 'ACTIVE'
           AND dataset_code = :dataset
           AND (
                (period_start IS NULL AND period_end IS NULL)
             OR (period_start IS NULL AND period_end > :periodStart)
             OR (period_end IS NULL AND period_start < :periodEnd)
             OR (period_start < :periodEnd AND period_end > :periodStart)
           )
        """,
        new MapSqlParameterSource()
            .addValue("dataset", datasetCode)
            .addValue("periodStart", periodStart)
            .addValue("periodEnd", periodEnd),
        Integer.class);
    return count != null && count > 0;
  }

  public UUID create(UUID tenantId, String datasetCode, String entityType, String entityId,
      LocalDate periodStart, LocalDate periodEnd, String reason, UUID createdBy) {
    UUID id = UUID.randomUUID();
    jdbc.update("""
        INSERT INTO archive_legal_hold
          (id, tenant_id, dataset_code, entity_type, entity_id, period_start, period_end,
           reason, created_by_actor_id)
        VALUES
          (:id, :tenantId, :dataset, :entityType, :entityId, :periodStart, :periodEnd,
           :reason, :createdBy)
        """,
        new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("tenantId", tenantId)
            .addValue("dataset", datasetCode)
            .addValue("entityType", entityType)
            .addValue("entityId", entityId)
            .addValue("periodStart", periodStart)
            .addValue("periodEnd", periodEnd)
            .addValue("reason", reason)
            .addValue("createdBy", createdBy));
    return id;
  }

  public void release(UUID id, UUID releasedBy, String releaseReason) {
    jdbc.update("""
        UPDATE archive_legal_hold
           SET status = 'RELEASED',
               released_by_actor_id = :releasedBy,
               released_at = now(),
               release_reason = :reason
         WHERE id = :id
           AND status = 'ACTIVE'
        """,
        new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("releasedBy", releasedBy)
            .addValue("reason", releaseReason));
  }

  public List<Map<String, Object>> listActive(int limit) {
    return jdbc.queryForList("""
        SELECT *
          FROM archive_legal_hold
         WHERE status = 'ACTIVE'
         ORDER BY created_at DESC
         LIMIT :limit
        """,
        Map.of("limit", limit));
  }
}
