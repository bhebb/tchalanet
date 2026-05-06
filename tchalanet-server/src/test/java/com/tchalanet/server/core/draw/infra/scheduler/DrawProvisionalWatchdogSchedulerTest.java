package com.tchalanet.server.core.draw.infra.scheduler;

import com.tchalanet.server.common.batch.context.BatchTchContextBinder;
import com.tchalanet.server.common.batch.exception.BatchSkippedException;
import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.common.batch.key.JobKey;
import com.tchalanet.server.common.batch.params.BatchParamKeys;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
import com.tchalanet.server.core.draw.infra.config.DrawProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DrawProvisionalWatchdogSchedulerTest {

  @Test
  void gateOffDoesNotQueriesDraws() {
    var drawReader = new RecordingDrawReader();
    var scheduler = scheduler(drawReader, new FixedBatchGate(false));

    assertThatThrownBy(scheduler::checkProvisionalStuck)
        .isInstanceOf(BatchSkippedException.class)
        .hasMessageContaining("Draw provisional watchdog gate disabled");

    assertThat(drawReader.calls).isZero();
  }

  @Test
  @Disabled
  void gateOnQueriesWithConfiguredDuration() {
    var drawReader = new RecordingDrawReader();
    var scheduler = scheduler(drawReader, new FixedBatchGate(true));

    scheduler.checkProvisionalStuck();

    assertThat(drawReader.calls).isEqualTo(1);
    assertThat(drawReader.lastDuration).isEqualTo(Duration.ofMinutes(45));
  }

  private DrawProvisionalWatchdogScheduler scheduler(
      DrawReaderPort drawReader, BatchGate batchGate) {
    var props = new DrawProperties();
    props.getWatchdog().setProvisionalStuckMinutes(45);
    return new DrawProvisionalWatchdogScheduler(
        drawReader,
        new SimpleMeterRegistry(),
        batchGate,
        props, new BatchTchContextBinder(null));
  }

  private static class FixedBatchGate extends BatchGate {
    private final boolean enabled;

    FixedBatchGate(boolean enabled) {
      super(null);
      this.enabled = enabled;
    }

    @Override
    public boolean enabled(JobKey jobKey, TenantId tenantId) {
      assertThat(jobKey).isEqualTo(BatchJobKeys.DRAW_WATCHDOG_PROVISIONAL);
      assertThat(tenantId).isNull();
      return enabled;
    }
  }

  private static class RecordingDrawReader implements DrawReaderPort {
    int calls;
    Duration lastDuration;

    @Override
    public boolean existsSettledDrawForResult(DrawResultId drawResultId) {
      return false;
    }

    @Override
    public List<DrawSummary> findByDrawResultId(DrawResultId drawResultId) {
      return List.of();
    }

    @Override
    public List<DrawSummary> findResultedWithProvisionalOlderThan(Duration duration) {
      calls++;
      lastDuration = duration;
      return List.of();
    }
  }

    private JobParameters jobParams() {

        return new JobParametersBuilder()
            .addString(BatchParamKeys.TENANT_ID, TenantId.of(UUID.randomUUID()).toString())
            .addString(BatchParamKeys.REQUEST_ID, UUID.randomUUID().toString())
            .addString(BatchParamKeys.ACTOR, "scheduler")
            .toJobParameters();
    }
}
