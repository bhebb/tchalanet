package com.tchalanet.server.platform.audit.internal.persistence;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditActorType;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.audit.internal.service.AuditEvent;
import com.tchalanet.server.platform.audit.internal.service.AuditEventReaderPort;
import com.tchalanet.server.platform.audit.internal.service.AuditEventWriterPort;
import com.tchalanet.server.platform.audit.internal.service.AuditEventsCriteria;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** JDBC operations on the canonical partitioned {@code audit_log} table. */
@Repository
@RequiredArgsConstructor
public class AuditLogJdbcRepository implements AuditEventReaderPort, AuditEventWriterPort {

  private static final String SELECT_COLUMNS =
      """
      SELECT id, tenant_id, occurred_at, actor_type, actor_id, entity_type, entity_id, action,
             details::text AS details_json, host(ip) AS ip, user_agent
        FROM audit_log
      """;

  private static final RowMapper<AuditEvent> ROW_MAPPER = AuditLogJdbcRepository::toDomain;

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public AuditEvent save(AuditEvent event) {
    UUID id = event.id() == null ? UUID.randomUUID() : event.id();
    jdbc.update(
        """
        INSERT INTO audit_log (
            id, tenant_id, occurred_at, business_date, actor_id, actor_type, action, entity_type,
            entity_id, severity, source, details, ip, user_agent
        ) VALUES (
            :id, :tenantId, :occurredAt, CAST(:occurredAt AS date), :actorId, :actorType, :action,
            :entityType, :entityId, 'INFO', 'API', CAST(:details AS jsonb), CAST(:ip AS inet),
            :userAgent
        )
        """,
        new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("tenantId", event.tenantId() == null ? null : event.tenantId().uuid())
            .addValue("occurredAt", Timestamp.from(event.occurredAt()))
            .addValue("actorId", event.actorId())
            .addValue("actorType", event.actorType().name())
            .addValue("action", event.action().name())
            .addValue("entityType", event.entityType().name())
            .addValue("entityId", event.entityId())
            .addValue("details", event.detailsJson())
            .addValue("ip", event.ip())
            .addValue("userAgent", event.userAgent()));
    return withId(event, id);
  }

  @Override
  public List<AuditEvent> findRecentForTenant(TenantId tenantId, int limit) {
    return jdbc.query(
        SELECT_COLUMNS
            + " WHERE tenant_id = :tenantId ORDER BY occurred_at DESC, id DESC LIMIT :limit",
        new MapSqlParameterSource()
            .addValue("tenantId", tenantId.uuid())
            .addValue("limit", Math.min(Math.max(limit, 1), 100)),
        ROW_MAPPER);
  }

  @Override
  public TchPage<AuditEvent> findByCriteria(AuditEventsCriteria criteria) {
    Pageable pageable =
        criteria == null || criteria.pageable() == null ? Pageable.ofSize(20) : criteria.pageable();
    int page = pageable.getPageNumber();
    int size = pageable.getPageSize();
    var query = buildCriteriaQuery(criteria);
    Long total =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit_log " + query.where(), query.params(), Long.class);
    long totalElements = total == null ? 0 : total;
    List<AuditEvent> items =
        jdbc.query(
            SELECT_COLUMNS
                + " "
                + query.where()
                + " ORDER BY occurred_at DESC, id DESC LIMIT :limit OFFSET :offset",
            query.params().addValue("limit", size).addValue("offset", (long) page * size),
            ROW_MAPPER);
    int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    boolean last = totalPages == 0 || page >= totalPages - 1;
    return TchPage.of(items, page, size, totalElements, totalPages, last, !last, page > 0);
  }

  @Override
  public int deleteBefore(Instant threshold) {
    return jdbc.update(
        "DELETE FROM audit_log WHERE occurred_at < :threshold",
        new MapSqlParameterSource("threshold", Timestamp.from(threshold)));
  }

  /** Count rows in [from, to) for the given tenant (null = all tenants). */
  public long countByPeriod(Instant from, Instant to, UUID tenantId) {
    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    MapSqlParameterSource params = periodParams(from, to);
    if (tenantId != null) params.addValue("tenantId", tenantId);
    Long count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit_log WHERE occurred_at >= :from AND occurred_at < :to"
                + tenantClause,
            params,
            Long.class);
    return count == null ? 0 : count;
  }

  /** Stream all rows in [from, to) for archive export. */
  public void streamByPeriod(
      Instant from, Instant to, UUID tenantId, Consumer<Map<String, Object>> consumer) {
    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    MapSqlParameterSource params = periodParams(from, to);
    if (tenantId != null) params.addValue("tenantId", tenantId);
    jdbc.query(
        "SELECT * FROM audit_log WHERE occurred_at >= :from AND occurred_at < :to"
            + tenantClause
            + " ORDER BY occurred_at",
        params,
        rs -> {
          var meta = rs.getMetaData();
          while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
              Object value = rs.getObject(i);
              if (value instanceof Timestamp timestamp) value = timestamp.toInstant();
              row.put(meta.getColumnLabel(i), value);
            }
            consumer.accept(row);
          }
          return null;
        });
  }

  /** Return UUID-backed entity rows for the coarse archive lookup index. */
  public List<Map<String, Object>> findDistinctEntityLookupRows(
      Instant from, Instant to, UUID tenantId, UUID archiveObjectId) {
    String tenantClause = tenantId != null ? " AND tenant_id = :tenantId" : "";
    MapSqlParameterSource params = periodParams(from, to);
    if (tenantId != null) params.addValue("tenantId", tenantId);
    List<Map<String, Object>> results = new ArrayList<>();
    jdbc.query(
        """
        SELECT tenant_id, entity_type, entity_id, MIN(occurred_at) AS occurred_at
          FROM audit_log
         WHERE occurred_at >= :from AND occurred_at < :to
           AND entity_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
        """
            + tenantClause
            + " GROUP BY tenant_id, entity_type, entity_id",
        params,
        rs -> {
          while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenant_id", rs.getObject("tenant_id", UUID.class));
            row.put("entity_type", rs.getString("entity_type"));
            row.put("entity_id", UUID.fromString(rs.getString("entity_id")));
            row.put("occurred_at", rs.getTimestamp("occurred_at").toInstant());
            results.add(row);
          }
          return null;
        });
    return results;
  }

  private CriteriaQuery buildCriteriaQuery(AuditEventsCriteria criteria) {
    var clauses = new ArrayList<String>();
    var params = new MapSqlParameterSource();
    if (criteria != null) {
      if (criteria.tenantId() != null) {
        clauses.add("tenant_id = :tenantId");
        params.addValue("tenantId", criteria.tenantId().uuid());
      }
      if (criteria.entityType() != null) {
        clauses.add("entity_type = :entityType");
        params.addValue("entityType", criteria.entityType().name());
      }
      if (criteria.entityId() != null && !criteria.entityId().isBlank()) {
        clauses.add("entity_id = :entityId");
        params.addValue("entityId", criteria.entityId().trim());
      }
      if (criteria.action() != null) {
        clauses.add("action = :action");
        params.addValue("action", criteria.action().name());
      }
      if (criteria.actorId() != null && !criteria.actorId().isBlank()) {
        clauses.add("CAST(actor_id AS text) = :actorId");
        params.addValue("actorId", criteria.actorId().trim());
      }
      if (criteria.ip() != null && !criteria.ip().isBlank()) {
        clauses.add("host(ip) = :ip");
        params.addValue("ip", criteria.ip().trim());
      }
      if (criteria.from() != null) {
        clauses.add("occurred_at >= :from");
        params.addValue("from", Timestamp.from(criteria.from()));
      }
      if (criteria.to() != null) {
        clauses.add("occurred_at <= :to");
        params.addValue("to", Timestamp.from(criteria.to()));
      }
    }
    return new CriteriaQuery(
        clauses.isEmpty() ? "" : "WHERE " + String.join(" AND ", clauses), params);
  }

  private static MapSqlParameterSource periodParams(Instant from, Instant to) {
    return new MapSqlParameterSource()
        .addValue("from", Timestamp.from(from))
        .addValue("to", Timestamp.from(to));
  }

  private static AuditEvent withId(AuditEvent event, UUID id) {
    return new AuditEvent(
        id,
        event.tenantId(),
        event.occurredAt(),
        event.actorType() == AuditActorType.USER ? event.actorId() : null,
        event.actorType(),
        event.actorId(),
        event.entityType(),
        event.entityId(),
        event.action(),
        event.detailsJson(),
        event.ip(),
        event.userAgent());
  }

  private static AuditEvent toDomain(ResultSet rs, int rowNum) throws SQLException {
    AuditActorType actorType = AuditActorType.valueOf(rs.getString("actor_type"));
    UUID actorId = rs.getObject("actor_id", UUID.class);
    return new AuditEvent(
        rs.getObject("id", UUID.class),
        TenantId.nullableOf(rs.getObject("tenant_id", UUID.class)),
        rs.getTimestamp("occurred_at").toInstant(),
        actorType == AuditActorType.USER ? actorId : null,
        actorType,
        actorId,
        AuditEntityType.valueOf(rs.getString("entity_type")),
        rs.getString("entity_id"),
        AuditAction.valueOf(rs.getString("action")),
        rs.getString("details_json"),
        rs.getString("ip"),
        rs.getString("user_agent"));
  }

  private record CriteriaQuery(String where, MapSqlParameterSource params) {}
}
