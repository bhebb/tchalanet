package com.tchalanet.server.draw.infra.batch.config;

import com.tchalanet.server.common.batch.config.BatchWindowsConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchJobScheduler {
  private final JobOperator jobOperator;
  private final Job fetchDrawResultsJob;
  private final Job settleDrawsJob;
  private final BatchWindowsConfig windows;
  private final Clock clock;

  // Job 1: fetch résultats (near real-time)
  @Scheduled(cron = "0 */5 * * * *") // toutes les 5 minutes
  public void scheduleFetchDrawResults() throws Exception {
    var now = LocalTime.now(clock);

    if (!windows.isInFetchResultsWindow(now)) {
      return;
    }
    jobOperator.start(fetchDrawResultsJob, buildParams());
  }

  // Job 2: settle tirages
  @Scheduled(cron = "0 */10 * * * *")
  public void scheduleSettleDraws() throws Exception {
    LocalTime now = LocalTime.now(clock);
    if (!windows.isInSettleDrawsWindow(now)) {
      return;
    }
    jobOperator.start(settleDrawsJob, buildParams());
  }

  private JobParameters buildParams() {
    return new JobParametersBuilder()
        .addLong("ts", Instant.now().toEpochMilli(), true)
        .toJobParameters();
  }
}
