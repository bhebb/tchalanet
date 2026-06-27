package com.tchalanet.server.app.batch.history;

import com.tchalanet.server.app.job.registry.SpringTchJobRegistry;
import com.tchalanet.server.common.job.history.BatchJobExecutionView;
import com.tchalanet.server.common.job.history.BatchJobHistoryPurgeResult;
import com.tchalanet.server.common.job.history.BatchJobHistoryService;
import com.tchalanet.server.common.job.key.JobKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class JdbcBatchJobHistoryService implements BatchJobHistoryService {

    private static final String RUNNING_STATUSES = "'STARTING','STARTED','STOPPING'";

    private final SpringTchJobRegistry registry;
    private final ApplicationContext applicationContext;
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcBatchJobHistoryService(
        SpringTchJobRegistry registry,
        ApplicationContext applicationContext,
        @Qualifier("batchDataSource") DataSource batchDataSource
    ) {
        this.registry = registry;
        this.applicationContext = applicationContext;
        this.jdbc = new NamedParameterJdbcTemplate(batchDataSource);
    }

    @Override
    public Optional<BatchJobExecutionView> getExecution(long executionId) {
        String sql = """
            SELECT
              e.JOB_EXECUTION_ID,
              i.JOB_NAME,
              e.STATUS,
              e.EXIT_CODE,
              e.EXIT_MESSAGE,
              e.START_TIME,
              e.END_TIME,
              %s AS CONTEXT
            FROM batch.BATCH_JOB_EXECUTION e
            JOIN batch.BATCH_JOB_INSTANCE i ON i.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID
            LEFT JOIN batch.BATCH_JOB_EXECUTION_PARAMS p
              ON p.JOB_EXECUTION_ID = e.JOB_EXECUTION_ID
            WHERE e.JOB_EXECUTION_ID = :executionId
            GROUP BY e.JOB_EXECUTION_ID, i.JOB_NAME, e.STATUS, e.EXIT_CODE, e.EXIT_MESSAGE, e.START_TIME, e.END_TIME
            """.formatted(CONTEXT_SQL);
        List<BatchJobExecutionView> rows = jdbc.query(
            sql,
            Map.of("executionId", executionId),
            this::mapExecution);
        return rows.stream().findFirst();
    }

    @Override
    public List<BatchJobExecutionView> listExecutions(JobKey jobKey, int limit) {
        String jobName = resolveBatchJobName(jobKey).orElse(null);
        if (jobName == null) {
            log.debug("batch.history.jobName.unavailable jobKey={}", jobKey);
            return List.of();
        }

        String sql = """
            SELECT
              e.JOB_EXECUTION_ID,
              i.JOB_NAME,
              e.STATUS,
              e.EXIT_CODE,
              e.EXIT_MESSAGE,
              e.START_TIME,
              e.END_TIME,
              %s AS CONTEXT
            FROM batch.BATCH_JOB_EXECUTION e
            JOIN batch.BATCH_JOB_INSTANCE i ON i.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID
            LEFT JOIN batch.BATCH_JOB_EXECUTION_PARAMS p
              ON p.JOB_EXECUTION_ID = e.JOB_EXECUTION_ID
            WHERE i.JOB_NAME = :jobName
            GROUP BY e.JOB_EXECUTION_ID, i.JOB_NAME, e.STATUS, e.EXIT_CODE, e.EXIT_MESSAGE, e.START_TIME, e.END_TIME
            ORDER BY COALESCE(e.START_TIME, e.CREATE_TIME) DESC
            LIMIT :limit
            """.formatted(CONTEXT_SQL);
        return jdbc.query(
            sql,
            Map.of("jobName", jobName, "limit", Math.max(1, Math.min(limit, 200))),
            this::mapExecution);
    }

    @Override
    @Transactional(transactionManager = "batchTxManager")
    public BatchJobHistoryPurgeResult purgeBefore(Instant cutoff) {
        if (cutoff == null) {
            throw new IllegalArgumentException("cutoff required");
        }

        var params = new MapSqlParameterSource("cutoff", Timestamp.from(cutoff));
        List<Long> executionIds = jdbc.queryForList("""
            SELECT JOB_EXECUTION_ID
            FROM batch.BATCH_JOB_EXECUTION
            WHERE CREATE_TIME < :cutoff
              AND (STATUS IS NULL OR STATUS NOT IN (%s))
            """.formatted(RUNNING_STATUSES), params, Long.class);

        if (executionIds.isEmpty()) {
            return new BatchJobHistoryPurgeResult(cutoff, 0, 0, 0, 0, 0, 0);
        }

        var deleteParams = new MapSqlParameterSource("executionIds", executionIds);
        List<Long> stepIds = jdbc.queryForList("""
            SELECT STEP_EXECUTION_ID
            FROM batch.BATCH_STEP_EXECUTION
            WHERE JOB_EXECUTION_ID IN (:executionIds)
            """, deleteParams, Long.class);

        List<Long> instanceIds = jdbc.queryForList("""
            SELECT DISTINCT JOB_INSTANCE_ID
            FROM batch.BATCH_JOB_EXECUTION
            WHERE JOB_EXECUTION_ID IN (:executionIds)
            """, deleteParams, Long.class);

        int stepContextRows = stepIds.isEmpty()
            ? 0
            : jdbc.update(
                "DELETE FROM batch.BATCH_STEP_EXECUTION_CONTEXT WHERE STEP_EXECUTION_ID IN (:stepIds)",
                new MapSqlParameterSource("stepIds", stepIds));
        int stepRows = jdbc.update(
            "DELETE FROM batch.BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID IN (:executionIds)",
            deleteParams);
        int jobContextRows = jdbc.update(
            "DELETE FROM batch.BATCH_JOB_EXECUTION_CONTEXT WHERE JOB_EXECUTION_ID IN (:executionIds)",
            deleteParams);
        int paramRows = jdbc.update(
            "DELETE FROM batch.BATCH_JOB_EXECUTION_PARAMS WHERE JOB_EXECUTION_ID IN (:executionIds)",
            deleteParams);
        int executionRows = jdbc.update(
            "DELETE FROM batch.BATCH_JOB_EXECUTION WHERE JOB_EXECUTION_ID IN (:executionIds)",
            deleteParams);
        int instanceRows = instanceIds.isEmpty()
            ? 0
            : jdbc.update("""
                DELETE FROM batch.BATCH_JOB_INSTANCE
                WHERE JOB_INSTANCE_ID IN (:instanceIds)
                  AND NOT EXISTS (
                    SELECT 1
                    FROM batch.BATCH_JOB_EXECUTION e
                    WHERE e.JOB_INSTANCE_ID = batch.BATCH_JOB_INSTANCE.JOB_INSTANCE_ID
                  )
                """, new MapSqlParameterSource("instanceIds", instanceIds));

        return new BatchJobHistoryPurgeResult(
            cutoff,
            jobContextRows,
            stepContextRows,
            stepRows,
            paramRows,
            executionRows,
            instanceRows);
    }

    private BatchJobExecutionView mapExecution(ResultSet rs, int rowNum) throws SQLException {
        String jobName = rs.getString("JOB_NAME");
        return new BatchJobExecutionView(
            rs.getLong("JOB_EXECUTION_ID"),
            resolveJobKey(jobName).orElse(jobName),
            jobName,
            rs.getString("STATUS"),
            toInstant(rs, "START_TIME"),
            toInstant(rs, "END_TIME"),
            trimToNull(rs.getString("CONTEXT")),
            trimToNull(rs.getString("EXIT_CODE")),
            compactExitMessage(rs.getString("EXIT_MESSAGE")));
    }

    private static final String CONTEXT_SQL = """
        COALESCE(
          NULLIF(MAX(CASE WHEN p.PARAMETER_NAME = 'slot_keys' THEN p.PARAMETER_VALUE END), ''),
          NULLIF(MAX(CASE WHEN p.PARAMETER_NAME = 'slot_key' THEN p.PARAMETER_VALUE END), ''),
          NULLIF(MAX(CASE WHEN p.PARAMETER_NAME = 'date' THEN p.PARAMETER_VALUE END), ''),
          NULLIF(MAX(CASE WHEN p.PARAMETER_NAME = 'from' THEN p.PARAMETER_VALUE END), '')
        )
        """;

    private Optional<String> resolveBatchJobName(JobKey jobKey) {
        return registry.findRuntime(jobKey)
            .flatMap(runtime -> {
                try {
                    Job job = applicationContext.getBean(runtime.springJobBeanName(), Job.class);
                    return Optional.of(job.getName());
                } catch (RuntimeException e) {
                    return Optional.empty();
                }
            });
    }

    private Optional<String> resolveJobKey(String jobName) {
        return registry.list().stream()
            .filter(registered -> resolveBatchJobName(registered.jobKey())
                .map(jobName::equals)
                .orElse(false))
            .map(registered -> registered.jobKey().value())
            .findFirst();
    }

    private static Instant toInstant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value != null ? value.toInstant(ZoneOffset.UTC) : null;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private static String compactExitMessage(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) return null;
        String firstLine = trimmed.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .findFirst()
            .orElse(trimmed);
        return firstLine.length() > 1000 ? firstLine.substring(0, 1000) + "…" : firstLine;
    }
}
