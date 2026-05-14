package com.tchalanet.server.common.batch.launch;

import com.tchalanet.server.common.batch.context.BatchTchContextBinder;
import com.tchalanet.server.common.batch.gate.BatchDisabledException;
import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.job.params.JobParamKeys;
import com.tchalanet.server.common.job.params.JobParamsValidator;
import com.tchalanet.server.common.batch.registry.RegisteredJob;
import com.tchalanet.server.common.batch.registry.TchBatchJobRegistry;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Starts batch jobs (Spring Batch 6).
 *
 * Responsibilities:
 * - Allowlist (TchBatchJobRegistry)
 * - Gate (BatchGate)
 * - Params validation (JobParamsValidator)
 * - Build JobParameters (+ ts)
 * - Resolve Job bean
 * - Start via JobOperator.start(Job, JobParameters)
 * Notes:
 * - JobLauncher is deprecated since SB6 in favor of JobOperator.
 * - We intentionally use a unique identifying "ts" parameter so Ops can launch
 *   the same job multiple times without JobInstance collision.
 *   Restart semantics should be handled by dedicated endpoints later if needed.
 */
@Component
@Slf4j
public class BatchJobStarter {

    private final TchBatchJobRegistry tchBatchJobRegistry;
    private final BatchGate gate;
    private final JobParamsValidator validator;
    private final JobOperator jobOperator;
    private final ApplicationContext applicationContext;
    private final Clock clock;
    private final BatchTchContextBinder binder;

    public BatchJobStarter(
        TchBatchJobRegistry tchBatchJobRegistry,
        BatchGate gate,
        JobParamsValidator validator,
        JobOperator jobOperator,
        ApplicationContext applicationContext,
        Clock clock,
        BatchTchContextBinder binder
    ) {
        this.tchBatchJobRegistry = tchBatchJobRegistry;
        this.gate = gate;
        this.validator = validator;
        this.jobOperator = jobOperator;
        this.applicationContext = applicationContext;
        this.clock = clock;
        this.binder = binder;
    }

    public JobExecution start(JobKey jobKey, Map<String, String> params) {
        if (jobKey == null) {
            throw new IllegalArgumentException("jobKey required");
        }
        if (params == null) {
            throw new IllegalArgumentException("params required");
        }

        log.info("batch.start.requested jobKey={} paramsKeys={}", jobKey, params.keySet());

        // 1) allowlist
        var registered = tchBatchJobRegistry.find(jobKey)
            .orElseThrow(() -> new IllegalArgumentException("Job not in allowlist: " + jobKey));

        // 2) tenantId if needed
        TenantId tenantId = null;
        if (registered.scope() == RegisteredJob.JobScope.TENANT) {
            String tenantIdStr = trimToNull(params.get(JobParamKeys.TENANT_ID));
            if (tenantIdStr == null) {
                throw new IllegalArgumentException("tenant_id required for TENANT job");
            }
            tenantId = TenantId.parse(tenantIdStr);
        }

        // 3) gate
        gate.assertEnabledOrThrow(jobKey, tenantId);

        // 4) validate
        validator.validate(jobKey, registered, params);

        // 5) job params
        var jobParameters = buildJobParameters(params);
        binder.bind(jobParameters);

        // 6) resolve Job bean
        Job job = resolveJobBean(registered.springJobBeanName());

        // 7) start via JobOperator
        try {
            JobExecution execution = jobOperator.start(job, jobParameters);

            log.info("batch.start.success jobKey={} jobName={} executionId={} status={}",
                jobKey, job.getName(), execution.getId(), execution.getStatus());

            return execution;
        } catch (BatchDisabledException e) {
            throw e;
        } catch (Exception e) {
            log.error("batch.start.failed jobKey={} jobBean={}", jobKey, registered.springJobBeanName(), e);
            throw new RuntimeException("Failed to start job: " + jobKey, e);
        }
    }

    private Job resolveJobBean(String springJobBeanName) {
        try {
            return applicationContext.getBean(springJobBeanName, Job.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Job bean not found: " + springJobBeanName, e);
        }
    }

    /**
     * Build JobParameters.
     *
     * Convention for Ops:
     * - All user params are NON-identifying (identifying=false)
     * - ts is IDENTIFYING to avoid collisions (identifying=true)
     * - request_id & actor are auto-filled if missing
     */
    private JobParameters buildJobParameters(Map<String, String> params) {
        JobParametersBuilder builder = new JobParametersBuilder();

        // user params (non-identifying)
        for (var entry : params.entrySet()) {
            String key = entry.getKey();
            String value = trimToNull(entry.getValue());
            if (value == null) continue;

            // never treat user params as identifying here
            if (!JobParamKeys.TS.equals(key)) {
                builder.addString(key, value, false);
            }
        }

        // request tracking defaults (non-identifying)
        if (trimToNull(params.get(JobParamKeys.REQUEST_ID)) == null) {
            builder.addString(JobParamKeys.REQUEST_ID, UUID.randomUUID().toString(), false);
        }
        if (trimToNull(params.get(JobParamKeys.ACTOR)) == null) {
            builder.addString(JobParamKeys.ACTOR, "ops", false);
        }

        // anti-collision (identifying)
        String tsRaw = trimToNull(params.get(JobParamKeys.TS));
        long ts = (tsRaw != null) ? parseTs(tsRaw) : Instant.now(clock).toEpochMilli();
        builder.addLong(JobParamKeys.TS, ts, true);

        return builder.toJobParameters();
    }

    private static String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static long parseTs(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ts (expected epoch millis): " + raw, e);
        }
    }
}
