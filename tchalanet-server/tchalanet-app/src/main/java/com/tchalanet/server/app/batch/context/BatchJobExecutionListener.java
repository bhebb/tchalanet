package com.tchalanet.server.app.batch.context;

import com.tchalanet.server.app.batch.params.SpringBatchJobParams;
import com.tchalanet.server.common.job.params.JobParamKeys;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchJobExecutionListener implements JobExecutionListener {

    private final SpringBatchJobContextBinder binder;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        var jobName = jobExecution.getJobInstance().getJobName();
        var executionId = jobExecution.getId();

        log.info("batch.job.start jobName={} executionId={}", jobName, executionId);
        var params = SpringBatchJobParams.toStringMap(jobExecution.getJobParameters());
        try {
            if (params.containsKey(JobParamKeys.TENANT_ID)) {
                binder.bindTenant(
                    TenantId.parse(params.get(JobParamKeys.TENANT_ID)),
                    params.getOrDefault(JobParamKeys.ACTOR, "unknown"));
            } else {
                binder.bindPlatform(params.getOrDefault(JobParamKeys.ACTOR, "unknown"));
            }
        } catch (Exception e) {
            try {
                binder.clear();
            } catch (Exception clearEx) {
                log.warn(
                    "batch.context.clear.afterBindFailure.failed jobName={} executionId={}",
                    jobName,
                    executionId,
                    clearEx
                );
            }

            log.error(
                "batch.context.bind.failed jobName={} executionId={}",
                jobName,
                executionId,
                e
            );

            throw e;
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        var jobName = jobExecution.getJobInstance().getJobName();
        var executionId = jobExecution.getId();
        BatchStatus status = jobExecution.getStatus();

        var start = jobExecution.getStartTime();
        var end = jobExecution.getEndTime();

        long durationMs = -1L;
        if (start != null && end != null) {
            durationMs = Duration.between(start, end).toMillis();
        }

        try {
            log.info(
                "batch.job.end jobName={} executionId={} status={} durationMs={}",
                jobName,
                executionId,
                status,
                durationMs
            );
        } finally {
            try {
                binder.clear();
            } catch (Exception e) {
                log.warn(
                    "batch.context.clear.failed jobName={} executionId={}",
                    jobName,
                    executionId,
                    e
                );
            }
        }
    }
}
