package com.tchalanet.server.app.batch.launch;

import com.tchalanet.server.common.job.launch.BatchJobExecutionOperator;
import com.tchalanet.server.common.job.launch.JobStartResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringBatchJobExecutionOperator implements BatchJobExecutionOperator {

    private final JobOperator jobOperator;
    private final JobRepository jobRepository;

    @Override
    public JobStartResult restart(long executionId) {
        try {
            var execution = jobRepository.getJobExecution(executionId);
            if (execution == null) {
                throw new IllegalArgumentException("Batch execution not found: " + executionId);
            }
            var restarted = jobOperator.restart(execution);
            log.info("batch.restart.success originalExecutionId={} restartedExecutionId={}",
                executionId,
                restarted.getId());
            return new JobStartResult(
                String.valueOf(restarted.getJobInstance().getInstanceId()),
                String.valueOf(restarted.getId()),
                restarted.getStatus().name());
        } catch (Exception e) {
            log.error("batch.restart.failed executionId={}", executionId, e);
            throw new RuntimeException("Failed to restart batch execution: " + executionId, e);
        }
    }
}
