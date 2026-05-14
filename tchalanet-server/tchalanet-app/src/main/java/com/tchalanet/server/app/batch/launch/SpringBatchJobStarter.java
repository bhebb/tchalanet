package com.tchalanet.server.app.batch.launch;

import com.tchalanet.server.app.batch.context.SpringBatchJobContextBinder;
import com.tchalanet.server.app.job.registry.SpringTchJobRegistry;
import com.tchalanet.server.app.batch.params.SpringBatchJobParams;
import com.tchalanet.server.common.job.gate.BatchDisabledException;
import com.tchalanet.server.common.job.context.JobContextBindingRequest;
import com.tchalanet.server.common.job.context.JobExecutionScope;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Spring Batch implementation of {@link BatchJobStarter}.
 *
 * <p>Responsibilities: allowlist (via {@link SpringTchJobRegistry}), gate
 * ({@link BatchGate}), required-params check, build identifying {@code ts}
 * parameter, resolve Job bean, start via {@link JobOperator}.
 */
@Component
@Slf4j
public class SpringBatchJobStarter implements BatchJobStarter {

  private final SpringTchJobRegistry registry;
  private final BatchGate gate;
  private final JobOperator jobOperator;
  private final ApplicationContext applicationContext;
  private final Clock clock;
  private final SpringBatchJobContextBinder binder;

  public SpringBatchJobStarter(
      SpringTchJobRegistry registry,
      BatchGate gate,
      JobOperator jobOperator,
      ApplicationContext applicationContext,
      Clock clock,
      SpringBatchJobContextBinder binder) {
    this.registry = registry;
    this.gate = gate;
    this.jobOperator = jobOperator;
    this.applicationContext = applicationContext;
    this.clock = clock;
    this.binder = binder;
  }

  @Override
  public JobStartResult start(JobKey jobKey, Map<String, String> params) {
    if (jobKey == null) throw new IllegalArgumentException("jobKey required");
    if (params == null) throw new IllegalArgumentException("params required");

    log.info("batch.start.requested jobKey={} paramsKeys={}", jobKey, params.keySet());

    var registered =
        registry
            .find(jobKey)
            .orElseThrow(() -> new IllegalArgumentException("Job not in allowlist: " + jobKey));

    TenantId tenantId = null;
    if (registered.scope() == RegisteredJob.JobScope.TENANT) {
      String tenantIdStr = trimToNull(params.get(JobParamKeys.TENANT_ID));
      if (tenantIdStr == null) {
        throw new IllegalArgumentException("tenant_id required for TENANT job");
      }
      tenantId = TenantId.parse(tenantIdStr);
    }

    gate.assertEnabledOrThrow(jobKey, tenantId);
    requireParams(jobKey, registered, params);

    var jobParameters = buildJobParameters(params);
    var scope = registered.scope() == RegisteredJob.JobScope.TENANT
        ? JobExecutionScope.TENANT
        : JobExecutionScope.PLATFORM;
    binder.bind(new JobContextBindingRequest(scope, SpringBatchJobParams.toStringMap(jobParameters)));

    Job job = resolveJobBean(registered.springJobBeanName());

    try {
      JobExecution execution = jobOperator.start(job, jobParameters);
      log.info(
          "batch.start.success jobKey={} jobName={} executionId={} status={}",
          jobKey,
          job.getName(),
          execution.getId(),
          execution.getStatus());
      return new JobStartResult(
          execution.getJobInstance() == null
              ? null
              : String.valueOf(execution.getJobInstance().getInstanceId()),
          String.valueOf(execution.getId()),
          execution.getStatus().name());
    } catch (BatchDisabledException e) {
      throw e;
    } catch (Exception e) {
      log.error("batch.start.failed jobKey={} jobBean={}", jobKey, registered.springJobBeanName(), e);
      throw new RuntimeException("Failed to start job: " + jobKey, e);
    }
  }

  private void requireParams(JobKey jobKey, RegisteredJob registered, Map<String, String> params) {
    for (String required : registered.requiredParams()) {
      if (trimToNull(params.get(required)) == null) {
        throw new IllegalArgumentException(
            "Required parameter missing for " + jobKey + ": " + required);
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
      String key = entry.getKey();
      String value = trimToNull(entry.getValue());
      if (value == null) continue;
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
