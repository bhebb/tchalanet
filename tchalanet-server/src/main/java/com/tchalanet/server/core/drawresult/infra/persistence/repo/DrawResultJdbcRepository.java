package com.tchalanet.server.core.drawresult.infra.persistence.repo;

import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.domain.model.DrawSource;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.drawresult.infra.persistence.DrawResultJpaEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DrawResultJdbcRepository {

  private final JdbcTemplate jdbc;
  private final JsonUtils jsonUtils;

  private static Object TimestampFromInstant(Instant i) {
    if (i == null) return null;
    return java.sql.Timestamp.from(i);
  }

  public UUID findByResultSlotIdAndOccurredAt(UUID resultSlotId, Instant occurredAt) {
    var sql =
        "select id from draw_result where result_slot_id = ? and occurred_at = ? and deleted_at is null";
    var rows =
        jdbc.query(
            sql,
            (rs, i) -> (UUID) rs.getObject("id"),
            resultSlotId,
            TimestampFromInstant(occurredAt));
    return rows.isEmpty() ? null : rows.get(0);
  }

  public long countByCriteria(
      String provider, String slotKey, java.time.LocalDate from, java.time.LocalDate to) {
    var sb = new StringBuilder("select count(1) from draw_result where deleted_at is null");
    var params = new java.util.ArrayList<Object>();
    // provider/slot criteria removed since columns not present in table
    if (from != null) {
      sb.append(" and occurred_at >= ?");
      params.add(java.sql.Timestamp.from(from.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()));
    }
    if (to != null) {
      sb.append(" and occurred_at <= ?");
      params.add(
          java.sql.Timestamp.from(
              to.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()));
    }
    Long c = jdbc.queryForObject(sb.toString(), params.toArray(), Long.class);
    return c == null ? 0L : c;
  }

  public List<DrawResultJpaEntity> findByCriteria(
      String provider,
      String slotKey,
      java.time.LocalDate from,
      java.time.LocalDate to,
      int limit,
      int offset) {
    var sb =
        new StringBuilder(
            "select id, result_slot_id, occurred_at, source_result, haiti_result, raw_payload, flags, status, quality, source, source_hash, fetched_at, override_reason from draw_result where deleted_at is null");
    var params = new ArrayList<Object>();

    if (from != null) {
      sb.append(" and occurred_at >= ?");
      params.add(java.sql.Timestamp.from(from.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()));
    }
    if (to != null) {
      sb.append(" and occurred_at <= ?");
      params.add(
          java.sql.Timestamp.from(
              to.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()));
    }
    sb.append(" order by occurred_at desc limit ? offset ?");
    params.add(limit);
    params.add(offset);

    return jdbc.query(
        sb.toString(),
        (rs, rowNum) -> {
          DrawResultJpaEntity e = new DrawResultJpaEntity();
          e.setId((UUID) rs.getObject("id"));
          e.setResultSlotId((UUID) rs.getObject("result_slot_id"));
          var occ = rs.getTimestamp("occurred_at");
          e.setOccurredAt(occ == null ? null : occ.toInstant());

          // parse json columns with JsonUtils (centralized mapper)
          String srcJson = rs.getString("source_result");
          e.setSourceResult(srcJson == null ? null : jsonUtils.parse(srcJson));

          String haitiJson = rs.getString("haiti_result");
          e.setHaitiResult(haitiJson == null ? null : jsonUtils.parse(haitiJson));

          String rawJson = rs.getString("raw_payload");
          e.setRawPayload(rawJson == null ? null : jsonUtils.parse(rawJson));

          String flagsJson = rs.getString("flags");
          e.setFlags(flagsJson == null ? null : jsonUtils.parse(flagsJson));

          e.setStatus(
              DrawResultStatus.valueOf(
                  rs.getString("status")));
          var q = rs.getString("quality");
          if (q != null)
            e.setQuality(com.tchalanet.server.common.types.enums.ResultQuality.valueOf(q));
          var src = rs.getString("source");
          if (src != null)
            e.setSource(DrawSource.valueOf(src));
          e.setSourceHash(rs.getString("source_hash"));
          var fetched = rs.getTimestamp("fetched_at");
          e.setFetchedAt(fetched == null ? null : fetched.toInstant());
          e.setOverrideReason(rs.getString("override_reason"));
          return e;
        },
        params.toArray());
  }
}
