package com.tchalanet.server.common.batch.context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Job execution listener that binds/unbinds TchRequestContext for TENANT-scoped jobs.
 * <p>
 * Usage:
 * - Add this listener to every TENANT-scoped batch job
 * - For GLOBAL jobs, do not add this listener
 * <p>
 * Responsibilities:
 * - beforeJob: bind context from JobParameters
 * - afterJob: clear context and log outcome
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchJobExecutionListener implements JobExecutionListener {

    private final BatchTchContextBinder binder;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        var jobName = jobExecution.getJobInstance().getJobName();
        var executionId = jobExecution.getId();

        log.info("batch.job.start jobName={} executionId={}", jobName, executionId);

        try {
            binder.bind(jobExecution.getJobParameters());
        } catch (Exception e) {
            // best-effort cleanup in case bind partially succeeded
            try {
                binder.clear();
            } catch (Exception clearEx) {
                log.warn("batch.context.clear.afterBindFailure.failed jobName={} executionId={}",
                    jobName, executionId, clearEx);
            }
            log.error("batch.context.bind.failed jobName={} executionId={}", jobName, executionId, e);
            throw e;
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        var jobName = jobExecution.getJobInstance().getJobName();
        var executionId = jobExecution.getId(); // ✅ compatible with core.job.JobExecution
        BatchStatus status = jobExecution.getStatus();

        var start = jobExecution.getStartTime(); // LocalDateTime in Batch 6
        var end = jobExecution.getEndTime();     // LocalDateTime in Batch 6

        long durationMs = -1L;
        if (start != null && end != null) {
            durationMs = Duration.between(start, end).toMillis();
        }

        try {
            log.info("batch.job.end jobName={} executionId={} status={} durationMs={}",
                jobName, executionId, status, durationMs);
        } finally {
            // ALWAYS clear the context
            try {
                binder.clear();
            } catch (Exception e) {
                log.warn("batch.context.clear.failed jobName={} executionId={}", jobName, executionId, e);
            }
        }
    }
}
