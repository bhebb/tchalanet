package com.tchalanet.server.core.analytics.internal.infra.archive;

import com.tchalanet.server.platform.archive.api.ArchiveDatasetProvider;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetKey;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetPlan;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportResult;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupEntry;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupResult;
import com.tchalanet.server.platform.archive.api.model.ArchivePeriod;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Slf4j
abstract class AbstractAnalyticsArchiveDatasetProvider implements ArchiveDatasetProvider {

  private static final int SCHEMA_VERSION = 1;
  private static final String ENTITY_TYPE = "ANALYTICS";

  private final ArchiveDatasetKey key;
  private final NamedParameterJdbcTemplate jdbc;

  protected AbstractAnalyticsArchiveDatasetProvider(
      NamedParameterJdbcTemplate jdbc, String tableName, String datasetName) {
    this.jdbc = jdbc;
    this.key = ArchiveDatasetKey.of(tableName, datasetName);
  }

  @Override
  public ArchiveDatasetKey key() {
    return key;
  }

  @Override
  public ArchiveDatasetPlan plan(ArchivePeriod period, UUID tenantId) {
    long count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
              FROM %s
             WHERE ref_date >= :start
               AND ref_date <  :end
               AND (:tenantId IS NULL OR tenant_id = :tenantId)
            """
                .formatted(key.tableName()),
            params(period, tenantId),
            Long.class);
    return new ArchiveDatasetPlan(key, period, tenantId, count, count > 0);
  }

  @Override
  public ArchiveExportResult export(ArchiveExportRequest request) {
    long[] exported = {0};
    jdbc.query(
        """
        SELECT *
          FROM %s
         WHERE ref_date >= :start
           AND ref_date <  :end
           AND (:tenantId IS NULL OR tenant_id = :tenantId)
         ORDER BY ref_date, id
        """
            .formatted(key.tableName()),
        params(request.period(), request.tenantId()),
        rs -> {
          request.rowSink().accept(row(rs));
          exported[0]++;
        });

    log.info(
        "{} archive export: {} rows period={}/{} tenant={}",
        key.tableName(),
        exported[0],
        request.period().start(),
        request.period().end(),
        request.tenantId());
    return new ArchiveExportResult(exported[0], SCHEMA_VERSION);
  }

  @Override
  public ArchiveLookupResult lookup(ArchiveLookupRequest request) {
    return ArchiveLookupResult.notFound();
  }

  @Override
  public List<ArchiveLookupEntry> generateLookupRows(
      ArchivePeriod period, UUID tenantId, UUID archiveObjectId) {
    return jdbc
        .queryForList(
            """
            SELECT id, tenant_id, ref_date
              FROM %s
             WHERE ref_date >= :start
               AND ref_date <  :end
               AND (:tenantId IS NULL OR tenant_id = :tenantId)
             ORDER BY ref_date, id
            """
                .formatted(key.tableName()),
            params(period, tenantId))
        .stream()
        .map(
            r ->
                new ArchiveLookupEntry(
                    key.tableName(),
                    (UUID) r.get("tenant_id"),
                    ENTITY_TYPE,
                    (UUID) r.get("id"),
                    null,
                    (LocalDate) r.get("ref_date"),
                    null,
                    archiveObjectId,
                    null,
                    null))
        .toList();
  }

  private static MapSqlParameterSource params(ArchivePeriod period, UUID tenantId) {
    return new MapSqlParameterSource()
        .addValue("start", period.start())
        .addValue("end", period.end())
        .addValue("tenantId", tenantId);
  }

  private static Map<String, Object> row(ResultSet rs) throws SQLException {
    ResultSetMetaData meta = rs.getMetaData();
    Map<String, Object> row = new LinkedHashMap<>();
    for (int i = 1; i <= meta.getColumnCount(); i++) {
      String column = meta.getColumnLabel(i);
      Object value = rs.getObject(i);
      row.put(column, archiveValue(value));
    }
    return row;
  }

  private static Object archiveValue(Object value) {
    if (value == null) {
      return null;
    }
    if ("org.postgresql.util.PGobject".equals(value.getClass().getName())) {
      return value.toString();
    }
    return value;
  }
}
