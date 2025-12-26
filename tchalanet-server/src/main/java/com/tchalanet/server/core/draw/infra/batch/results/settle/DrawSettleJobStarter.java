package com.tchalanet.server.core.draw.infra.batch.results.settle;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawSettleJobStarter {

  private final JobOperator jobOperator;

  private final Job settleDrawsJob;

  @SneakyThrows
  public JobExecution startSettleDrawsJob(Map<String, String> params) {
    var jobParameters = buildJobParameters(params);
    return jobOperator.start(settleDrawsJob, jobParameters);
  }

  private JobParameters buildJobParameters(Map<String, String> params) {
    // require tenant
    if (params.get("tenant_id") == null || params.get("tenant_id").isBlank()) {
      throw new IllegalArgumentException("tenant_id is required for settle job");
    }

    JobParametersBuilder builder = new JobParametersBuilder();
    if (params.containsKey("ts")) {
      builder.addLong("ts", Long.parseLong(params.get("ts")), true);
    }
    addString(builder, "tenant_id", params.get("tenant_id"));
    addString(builder, "channel_code", params.get("channel_code"));
    addString(builder, "force", params.get("force"));
    if (params.containsKey("days_back")) {
      builder.addLong("days_back", Long.parseLong(params.get("days_back")), false);
    }
    if (params.containsKey("max_draws")) {
      builder.addLong("max_draws", Long.parseLong(params.get("max_draws")), false);
    }
    addString(builder, "dry_run", params.get("dry_run"));
    return builder.toJobParameters();
  }

  private void addString(JobParametersBuilder builder, String key, String value) {
    if (value != null && !value.isBlank()) builder.addString(key, value);
  }
}
