package com.tchalanet.server.app.batch.history;

import com.tchalanet.server.platform.archive.api.ArchiveDatasetProvider;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetKey;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetPlan;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportResult;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupEntry;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupResult;
import com.tchalanet.server.platform.archive.api.model.ArchivePeriod;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BatchJobExecutionArchiveDatasetProvider implements ArchiveDatasetProvider {

  static final int SCHEMA_VERSION = 1;
  static final String DATASET = "batch_job_execution";

  private static final ArchiveDatasetKey KEY =
      ArchiveDatasetKey.of(DATASET, "Spring Batch Job Executions");
  private static final String RUNNING_STATUSES = "'STARTING','STARTED','STOPPING'";

  private final NamedParameterJdbcTemplate jdbc;

  public BatchJobExecutionArchiveDatasetProvider(
      @Qualifier("batchDataSource") DataSource batchDataSource) {
    this.jdbc = new NamedParameterJdbcTemplate(batchDataSource);
  }

  @Override
  public ArchiveDatasetKey key() {
    return KEY;
  }

  @Override
  public ArchiveDatasetPlan plan(ArchivePeriod period, UUID tenantId) {
    long count = jdbc.queryForObject("""
        SELECT COUNT(*)
          FROM batch.BATCH_JOB_EXECUTION e
         WHERE e.CREATE_TIME >= :from
           AND e.CREATE_TIME < :to
           AND (e.STATUS IS NULL OR e.STATUS NOT IN (%s))
        """.formatted(RUNNING_STATUSES), params(period), Long.class);
    return new ArchiveDatasetPlan(KEY, period, tenantId, count, count > 0);
  }

  @Override
  public ArchiveExportResult export(ArchiveExportRequest request) {
    long[] exported = {0};
    jdbc.query("""
        SELECT
          e.JOB_EXECUTION_ID AS job_execution_id,
          e.JOB_INSTANCE_ID AS job_instance_id,
          i.JOB_NAME AS job_name,
          i.JOB_KEY AS job_key,
          e.VERSION AS version,
          e.CREATE_TIME AS create_time,
          e.START_TIME AS start_time,
          e.END_TIME AS end_time,
          e.STATUS AS status,
          e.EXIT_CODE AS exit_code,
          e.EXIT_MESSAGE AS exit_message,
          e.LAST_UPDATED AS last_updated,
          COALESCE((
            SELECT jsonb_agg(jsonb_build_object(
              'parameter_name', p.PARAMETER_NAME,
              'parameter_type', p.PARAMETER_TYPE,
              'parameter_value', p.PARAMETER_VALUE,
              'identifying', p.IDENTIFYING
            ) ORDER BY p.PARAMETER_NAME)
            FROM batch.BATCH_JOB_EXECUTION_PARAMS p
            WHERE p.JOB_EXECUTION_ID = e.JOB_EXECUTION_ID
          ), '[]'::jsonb)::text AS params_json,
          COALESCE((
            SELECT jsonb_build_object(
              'short_context', c.SHORT_CONTEXT,
              'serialized_context', c.SERIALIZED_CONTEXT
            )
            FROM batch.BATCH_JOB_EXECUTION_CONTEXT c
            WHERE c.JOB_EXECUTION_ID = e.JOB_EXECUTION_ID
          ), '{}'::jsonb)::text AS job_context_json,
          COALESCE((
            SELECT jsonb_agg(jsonb_build_object(
              'step_execution_id', s.STEP_EXECUTION_ID,
              'step_name', s.STEP_NAME,
              'version', s.VERSION,
              'create_time', s.CREATE_TIME,
              'start_time', s.START_TIME,
              'end_time', s.END_TIME,
              'status', s.STATUS,
              'commit_count', s.COMMIT_COUNT,
              'read_count', s.READ_COUNT,
              'filter_count', s.FILTER_COUNT,
              'write_count', s.WRITE_COUNT,
              'read_skip_count', s.READ_SKIP_COUNT,
              'write_skip_count', s.WRITE_SKIP_COUNT,
              'process_skip_count', s.PROCESS_SKIP_COUNT,
              'rollback_count', s.ROLLBACK_COUNT,
              'exit_code', s.EXIT_CODE,
              'exit_message', s.EXIT_MESSAGE,
              'last_updated', s.LAST_UPDATED,
              'step_context', COALESCE((
                SELECT jsonb_build_object(
                  'short_context', sc.SHORT_CONTEXT,
                  'serialized_context', sc.SERIALIZED_CONTEXT
                )
                FROM batch.BATCH_STEP_EXECUTION_CONTEXT sc
                WHERE sc.STEP_EXECUTION_ID = s.STEP_EXECUTION_ID
              ), '{}'::jsonb)
            ) ORDER BY s.STEP_EXECUTION_ID)
            FROM batch.BATCH_STEP_EXECUTION s
            WHERE s.JOB_EXECUTION_ID = e.JOB_EXECUTION_ID
          ), '[]'::jsonb)::text AS steps_json
        FROM batch.BATCH_JOB_EXECUTION e
        JOIN batch.BATCH_JOB_INSTANCE i ON i.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID
        WHERE e.CREATE_TIME >= :from
          AND e.CREATE_TIME < :to
          AND (e.STATUS IS NULL OR e.STATUS NOT IN (%s))
        ORDER BY e.CREATE_TIME, e.JOB_EXECUTION_ID
        """.formatted(RUNNING_STATUSES), params(request.period()), rs -> {
          request.rowSink().accept(Map.ofEntries(
              Map.entry("job_execution_id", rs.getLong("job_execution_id")),
              Map.entry("job_instance_id", rs.getLong("job_instance_id")),
              Map.entry("job_name", value(rs.getString("job_name"))),
              Map.entry("job_key", value(rs.getString("job_key"))),
              Map.entry("version", rs.getLong("version")),
              Map.entry("create_time", value(rs.getTimestamp("create_time"))),
              Map.entry("start_time", value(rs.getTimestamp("start_time"))),
              Map.entry("end_time", value(rs.getTimestamp("end_time"))),
              Map.entry("status", value(rs.getString("status"))),
              Map.entry("exit_code", value(rs.getString("exit_code"))),
              Map.entry("exit_message", value(rs.getString("exit_message"))),
              Map.entry("last_updated", value(rs.getTimestamp("last_updated"))),
              Map.entry("params_json", rs.getString("params_json")),
              Map.entry("job_context_json", rs.getString("job_context_json")),
              Map.entry("steps_json", rs.getString("steps_json"))));
          exported[0]++;
        });

    log.info("batch archive export: {} executions period={}/{}",
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
    Instant from = period.start().atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to = period.end().atStartOfDay(ZoneOffset.UTC).toInstant();
    return new MapSqlParameterSource()
        .addValue("from", Timestamp.from(from))
        .addValue("to", Timestamp.from(to));
  }

  private static Object value(Timestamp value) {
    return value == null ? "" : value.toInstant();
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }
}
