package com.tchalanet.server.app.batch.launch;

import com.tchalanet.server.app.job.registry.SpringTchJobRegistry;
import com.tchalanet.server.common.job.gate.BatchDisabledException;
import com.tchalanet.server.common.job.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.job.launch.BatchJobStarter;
import com.tchalanet.server.common.job.launch.JobStartResult;
import com.tchalanet.server.common.job.params.JobParamKeys;
import com.tchalanet.server.common.job.registry.RegisteredJob;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringBatchJobStarter implements BatchJobStarter {

    private final SpringTchJobRegistry registry;
    private final BatchGate gate;
    private final JobOperator jobOperator;
    private final ApplicationContext applicationContext;
    private final Clock clock;

    @Override
    public JobStartResult start(JobKey jobKey, Map<String, String> params) {
        if (jobKey == null) {
            throw new IllegalArgumentException("jobKey required");
        }
        if (params == null) {
            throw new IllegalArgumentException("params required");
        }

        log.info("batch.start.requested jobKey={} paramsKeys={}", jobKey, params.keySet());

        var runtimeJob = registry.findRuntime(jobKey)
            .orElseThrow(() -> new IllegalArgumentException("Job not in allowlist: " + jobKey));

        var metadata = runtimeJob.metadata();

        TenantId tenantId = resolveTenantIdIfRequired(metadata, params);

        gate.assertEnabledOrThrow(jobKey, tenantId);
        requireParams(jobKey, metadata, params);

        var jobParameters = buildJobParameters(params);
        var job = resolveJobBean(runtimeJob.springJobBeanName());

        try {
            JobExecution execution = jobOperator.start(job, jobParameters);

            log.info(
                "batch.start.success jobKey={} jobName={} executionId={} status={}",
                jobKey,
                job.getName(),
                execution.getId(),
                execution.getStatus()
            );

            return new JobStartResult(
                String.valueOf(execution.getJobInstance().getInstanceId()),
                String.valueOf(execution.getId()),
                execution.getStatus().name()
            );

        } catch (BatchDisabledException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                "batch.start.failed jobKey={} jobBean={}",
                jobKey,
                runtimeJob.springJobBeanName(),
                e
            );
            throw new RuntimeException("Failed to start job: " + jobKey, e);
        }
    }

    private TenantId resolveTenantIdIfRequired(RegisteredJob registered, Map<String, String> params) {
        if (registered.scope() != RegisteredJob.JobScope.TENANT) {
            return null;
        }

        var tenantIdRaw = trimToNull(params.get(JobParamKeys.TENANT_ID));
        if (tenantIdRaw == null) {
            throw new IllegalArgumentException("tenant_id required for TENANT job");
        }

        return TenantId.parse(tenantIdRaw);
    }

    private void requireParams(JobKey jobKey, RegisteredJob registered, Map<String, String> params) {
        for (String required : registered.requiredParams()) {
            if (trimToNull(params.get(required)) == null) {
                throw new IllegalArgumentException("Required parameter missing for " + jobKey + ": " + required);
            }
        }
    }

    private Job resolveJobBean(String springJobBeanName) {
        try {
            return applicationContext.getBean(springJobBeanName, Job.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Job bean not found: " + springJobBeanName, e);
        }
    }

    private JobParameters buildJobParameters(Map<String, String> params) {
        JobParametersBuilder builder = new JobParametersBuilder();

        for (var entry : params.entrySet()) {
            var key = entry.getKey();
            var value = trimToNull(entry.getValue());

            if (value == null) {
                continue;
            }

            if (!JobParamKeys.TS.equals(key)) {
                builder.addString(key, value, false);
            }
        }

        if (trimToNull(params.get(JobParamKeys.REQUEST_ID)) == null) {
            builder.addString(JobParamKeys.REQUEST_ID, UUID.randomUUID().toString(), false);
        }

        if (trimToNull(params.get(JobParamKeys.ACTOR)) == null) {
            builder.addString(JobParamKeys.ACTOR, "ops", false);
        }

        var tsRaw = trimToNull(params.get(JobParamKeys.TS));
        var ts = tsRaw != null ? parseTs(tsRaw) : Instant.now(clock).toEpochMilli();

        builder.addLong(JobParamKeys.TS, ts, true);

        return builder.toJobParameters();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static long parseTs(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ts, expected epoch millis: " + raw, e);
        }
    }
}
