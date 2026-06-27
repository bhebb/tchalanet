package com.tchalanet.server.platform.entityhistory.internal.service;

import com.tchalanet.server.common.web.paging.TchPage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EntityRevisionHistoryService {

  private static final List<ExposedEntity> EXPOSED_ENTITIES = List.of(
      new ExposedEntity(
          TechnicalRevisionEntityType.SELLER_TERMINAL,
          "seller_terminal_aud",
          "",
          "lower(aud.terminal_code) = lower(:query)",
          List.of(
              new ExposedField("terminal_code", "terminal_code_mod", "terminalCode"),
              new ExposedField("status", "status_mod", "status"),
              new ExposedField("commission_rate", "commission_rate_mod", "commissionRate"),
              new ExposedField("blocked_at", "blocked_at_mod", "blockedAt"),
              new ExposedField("blocked_by", "blocked_by_mod", "blockedBy"),
              new ExposedField("blocked_reason", "blocked_reason_mod", "blockedReason"),
              new ExposedField("disabled_at", "disabled_at_mod", "disabledAt"),
              new ExposedField("must_change_pin", "must_change_pin_mod", "mustChangePin"),
              new ExposedField("pin_reset_at", "pin_reset_at_mod", "pinResetAt"))),
      new ExposedEntity(
          TechnicalRevisionEntityType.DRAW_RESULT,
          "draw_result_aud",
          "JOIN result_slot rs ON rs.id = aud.result_slot_id",
          """
          aud.source_hash = :query
          OR aud.result_date::text = :query
          OR lower(rs.slot_key) = lower(:query)
          OR (
            :drawSlotKey IS NOT NULL
            AND :drawResultDate IS NOT NULL
            AND lower(rs.slot_key) = lower(:drawSlotKey)
            AND aud.result_date = :drawResultDate
          )
          """,
          List.of(
              new ExposedField("result_slot_id", null, "resultSlotId"),
              new ExposedField("result_date", null, "resultDate"),
              new ExposedField("occurred_at", null, "occurredAt"),
              new ExposedField("status", null, "status"),
              new ExposedField("quality", null, "quality"),
              new ExposedField("source", null, "source"),
              new ExposedField("source_hash", null, "sourceHash"),
              new ExposedField("override_reason", null, "overrideReason"))),
      new ExposedEntity(
          TechnicalRevisionEntityType.LIMIT_ASSIGNMENT,
          "limit_assignment_aud",
          "",
          "lower(aud.rule_key) = lower(:query) OR aud.scope_id::text = :query",
          List.of(
              new ExposedField("rule_key", null, "ruleKey"),
              new ExposedField("scope_type", null, "scopeType"),
              new ExposedField("scope_id", null, "scopeId"),
              new ExposedField("enabled", null, "enabled"),
              new ExposedField("on_breach", null, "onBreach"),
              new ExposedField("starts_at", null, "startsAt"),
              new ExposedField("ends_at", null, "endsAt"))));

  private final NamedParameterJdbcTemplate jdbc;

  public TchPage<EntityRevisionItem> listRevisions(
      TechnicalRevisionEntityType entityType,
      String query,
      Pageable pageable) {
    var exposed = resolveEntity(entityType);
    var normalizedQuery = normalizeQuery(query);
    var entityId = parseUuid(normalizedQuery);
    var drawSearch = parseDrawSearch(normalizedQuery);
    var params = new MapSqlParameterSource()
        .addValue("entityId", entityId)
        .addValue("query", normalizedQuery)
        .addValue("drawSlotKey", drawSearch.slotKey(), Types.VARCHAR)
        .addValue("drawResultDate", drawSearch.resultDate(), Types.DATE)
        .addValue("limit", pageable.getPageSize())
        .addValue("offset", pageable.getOffset());
    var whereClause = whereClause(exposed, entityId);

    var total = count(exposed, whereClause, params);
    var items = jdbc.query(
        """
        SELECT *
          FROM (
            SELECT aud.*,
                   rev.rev_timestamp,
                   rev.user_id,
                   rev.tenant_id AS revision_tenant_id
                   %s
              FROM %s aud
              %s
              JOIN revinfo rev ON rev.rev = aud.rev
             WHERE %s
          ) revisions
         ORDER BY rev DESC
         LIMIT :limit OFFSET :offset
        """.formatted(
            revisionValueProjection(exposed),
            exposed.tableName(),
            exposed.joinClause(),
            whereClause),
        params,
        (rs, rowNum) -> toItem(exposed, rs));

    var totalPages = total == 0 ? 1 : (int) Math.ceil((double) total / pageable.getPageSize());
    return TchPage.of(
        items,
        pageable.getPageNumber(),
        pageable.getPageSize(),
        total,
        totalPages,
        pageable.getPageNumber() + 1 >= totalPages,
        pageable.getPageNumber() + 1 < totalPages,
        pageable.getPageNumber() > 0);
  }

  private String whereClause(ExposedEntity exposed, UUID entityId) {
    if (entityId == null) {
      return "(" + exposed.businessSearchPredicate() + ")";
    }
    return "(aud.id = :entityId OR " + exposed.businessSearchPredicate() + ")";
  }

  private long count(ExposedEntity exposed, String whereClause, MapSqlParameterSource params) {
    var total = jdbc.queryForObject(
        "SELECT count(*) FROM %s aud %s WHERE %s".formatted(
            exposed.tableName(),
            exposed.joinClause(),
            whereClause),
        params,
        Long.class);
    return total == null ? 0 : total;
  }

  private EntityRevisionItem toItem(ExposedEntity exposed, ResultSet rs) throws SQLException {
    var changes = changedValues(exposed, rs);
    return new EntityRevisionItem(
        String.valueOf(rs.getInt("rev")),
        exposed.entityType().name(),
        rs.getObject("id", UUID.class).toString(),
        operation(rs.getShort("revtype")),
        Instant.ofEpochMilli(rs.getLong("rev_timestamp")),
        rs.getObject("user_id", UUID.class),
        tenantId(rs),
        changes.stream().map(EntityRevisionFieldChange::field).toList(),
        changes);
  }

  private UUID tenantId(ResultSet rs) throws SQLException {
    var tenantId = rs.getObject("tenant_id", UUID.class);
    return tenantId == null ? rs.getObject("revision_tenant_id", UUID.class) : tenantId;
  }

  private List<EntityRevisionFieldChange> changedValues(ExposedEntity exposed, ResultSet rs) {
    var changes = new ArrayList<EntityRevisionFieldChange>();
    for (int i = 0; i < exposed.fields().size(); i++) {
      var field = exposed.fields().get(i);
      if (!isChanged(rs, field, i)) continue;
      changes.add(new EntityRevisionFieldChange(
          field.apiName(),
          readString(rs, beforeAlias(i)),
          readString(rs, afterAlias(i))));
    }
    return changes;
  }

  private boolean isChanged(ResultSet rs, ExposedField field, int index) {
    if (field.modifiedFlagColumn() != null) {
      return readBoolean(rs, field.modifiedFlagColumn());
    }

    var before = readString(rs, beforeAlias(index));
    var after = readString(rs, afterAlias(index));
    return !Objects.equals(before, after);
  }

  private boolean readBoolean(ResultSet rs, String columnName) {
    try {
      return rs.getBoolean(columnName);
    } catch (SQLException ignored) {
      return false;
    }
  }

  private String readString(ResultSet rs, String columnName) {
    try {
      return rs.getString(columnName);
    } catch (SQLException ignored) {
      return null;
    }
  }

  private String revisionValueProjection(ExposedEntity exposed) {
    var projection = new StringBuilder();
    for (int i = 0; i < exposed.fields().size(); i++) {
      var field = exposed.fields().get(i);
      projection
          .append(", aud.")
          .append(field.columnName())
          .append("::text AS ")
          .append(afterAlias(i))
          .append(", lag(aud.")
          .append(field.columnName())
          .append("::text) OVER (PARTITION BY aud.id ORDER BY aud.rev) AS ")
          .append(beforeAlias(i));
    }
    return projection.toString();
  }

  private String beforeAlias(int index) {
    return "field_" + index + "_before";
  }

  private String afterAlias(int index) {
    return "field_" + index + "_after";
  }

  private String operation(short revType) {
    return switch (revType) {
      case 0 -> "CREATE";
      case 1 -> "UPDATE";
      case 2 -> "DELETE";
      default -> "UPDATE";
    };
  }

  private ExposedEntity resolveEntity(TechnicalRevisionEntityType entityType) {
    return EXPOSED_ENTITIES.stream()
        .filter(entity -> entity.entityType() == entityType)
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Unsupported entity history type: " + entityType));
  }

  private String normalizeQuery(String query) {
    if (query == null || query.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entity history query is required");
    }
    return query.trim();
  }

  private UUID parseUuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  private DrawSearch parseDrawSearch(String query) {
    var parts = query.split("[,;:/\\s]+");
    String slotKey = null;
    LocalDate resultDate = null;

    for (String part : parts) {
      if (part.isBlank()) continue;
      var parsedDate = parseDate(part);
      if (parsedDate != null) {
        resultDate = parsedDate;
      } else if (slotKey == null) {
        slotKey = part;
      }
    }

    return new DrawSearch(slotKey, resultDate);
  }

  private LocalDate parseDate(String value) {
    try {
      return LocalDate.parse(value);
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private record ExposedEntity(
      TechnicalRevisionEntityType entityType,
      String tableName,
      String joinClause,
      String businessSearchPredicate,
      List<ExposedField> fields) {}

  private record ExposedField(String columnName, String modifiedFlagColumn, String apiName) {}

  private record DrawSearch(String slotKey, LocalDate resultDate) {}
}
