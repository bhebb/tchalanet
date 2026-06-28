package com.tchalanet.server.platform.entityhistory.internal.archive;

import com.tchalanet.server.platform.archive.api.ArchiveDatasetProvider;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetKey;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetPlan;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportResult;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupEntry;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupResult;
import com.tchalanet.server.platform.archive.api.model.ArchivePeriod;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntityRevisionArchiveDatasetProvider implements ArchiveDatasetProvider {

  static final int SCHEMA_VERSION = 1;
  static final String DATASET = "entity_revision";

  private static final ArchiveDatasetKey KEY =
      ArchiveDatasetKey.of(DATASET, "Entity Revision History");

  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public ArchiveDatasetKey key() {
    return KEY;
  }

  @Override
  public ArchiveDatasetPlan plan(ArchivePeriod period, UUID tenantId) {
    long count = jdbc.queryForObject("""
        SELECT COUNT(*)
          FROM revinfo
         WHERE rev_timestamp >= :fromMillis
           AND rev_timestamp < :toMillis
        """, params(period), Long.class);
    return new ArchiveDatasetPlan(KEY, period, tenantId, count, count > 0);
  }

  @Override
  public ArchiveExportResult export(ArchiveExportRequest request) {
    long[] exported = {0};
    jdbc.query("""
        SELECT
          r.rev,
          r.rev_timestamp,
          to_timestamp(r.rev_timestamp / 1000.0) AT TIME ZONE 'UTC' AS revision_time,
          r.tenant_id,
          r.user_id,
          r.request_id,
          r.actor_type,
          r.api_scope,
          r.tenant_overridden,
          COALESCE((
            SELECT jsonb_agg(to_jsonb(a) ORDER BY a.id)
            FROM draw_result_aud a
            WHERE a.rev = r.rev
          ), '[]'::jsonb)::text AS draw_result_aud_json,
          COALESCE((
            SELECT jsonb_agg(to_jsonb(a) ORDER BY a.id)
            FROM limit_assignment_aud a
            WHERE a.rev = r.rev
          ), '[]'::jsonb)::text AS limit_assignment_aud_json,
          COALESCE((
            SELECT jsonb_agg(to_jsonb(a) ORDER BY a.id)
            FROM seller_terminal_aud a
            WHERE a.rev = r.rev
          ), '[]'::jsonb)::text AS seller_terminal_aud_json
        FROM revinfo r
        WHERE r.rev_timestamp >= :fromMillis
          AND r.rev_timestamp < :toMillis
        ORDER BY r.rev_timestamp, r.rev
        """, params(request.period()), rs -> {
          request.rowSink().accept(Map.ofEntries(
              Map.entry("rev", rs.getInt("rev")),
              Map.entry("rev_timestamp", rs.getLong("rev_timestamp")),
              Map.entry("revision_time", rs.getTimestamp("revision_time").toInstant()),
              Map.entry("tenant_id", value(rs.getObject("tenant_id"))),
              Map.entry("user_id", value(rs.getObject("user_id"))),
              Map.entry("request_id", value(rs.getString("request_id"))),
              Map.entry("actor_type", value(rs.getString("actor_type"))),
              Map.entry("api_scope", value(rs.getString("api_scope"))),
              Map.entry("tenant_overridden", rs.getBoolean("tenant_overridden")),
              Map.entry("draw_result_aud_json", rs.getString("draw_result_aud_json")),
              Map.entry("limit_assignment_aud_json", rs.getString("limit_assignment_aud_json")),
              Map.entry("seller_terminal_aud_json", rs.getString("seller_terminal_aud_json"))));
          exported[0]++;
        });

    log.info("entity revision archive export: {} revisions period={}/{}",
        exported[0], request.period().start(), request.period().end());
    return new ArchiveExportResult(exported[0], SCHEMA_VERSION);
  }

  @Override
  public ArchiveLookupResult lookup(ArchiveLookupRequest request) {
    return ArchiveLookupResult.notFound();
  }

  @Override
  public List<ArchiveLookupEntry> generateLookupRows(
      ArchivePeriod period, UUID tenantId, UUID archiveObjectId) {
    return List.of();
  }

  private static MapSqlParameterSource params(ArchivePeriod period) {
    long fromMillis = period.start().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    long toMillis = period.end().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    return new MapSqlParameterSource()
        .addValue("fromMillis", fromMillis)
        .addValue("toMillis", toMillis);
  }

  private static Object value(Object value) {
    return value == null ? "" : value;
  }
}
