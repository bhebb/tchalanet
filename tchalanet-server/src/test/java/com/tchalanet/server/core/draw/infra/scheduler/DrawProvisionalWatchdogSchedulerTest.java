package com.tchalanet.server.core.draw.infra.scheduler;

import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.common.batch.key.JobKey;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.DrawReaderPort;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import com.tchalanet.server.core.draw.infra.config.DrawProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DrawProvisionalWatchdogSchedulerTest {

  @Test
  void gateOffDoesNotQueryDraws() {
    var drawReader = new RecordingDrawReader();
    var scheduler = scheduler(drawReader, new FixedBatchGate(false));

    scheduler.checkProvisionalStuck();

    assertThat(drawReader.calls).isZero();
  }

  @Test
  void gateOnQueriesWithConfiguredDuration() {
    var drawReader = new RecordingDrawReader();
    var scheduler = scheduler(drawReader, new FixedBatchGate(true));

    scheduler.checkProvisionalStuck();

    assertThat(drawReader.calls).isEqualTo(1);
    assertThat(drawReader.lastDuration).isEqualTo(Duration.ofMinutes(45));
  }

  private static DrawProvisionalWatchdogScheduler scheduler(
      DrawReaderPort drawReader, BatchGate batchGate) {
    var props = new DrawProperties();
    props.getWatchdog().setProvisionalStuckMinutes(45);
    return new DrawProvisionalWatchdogScheduler(
        drawReader,
        new SimpleMeterRegistry(),
        batchGate,
        Clock.fixed(Instant.parse("2026-04-28T12:00:00Z"), ZoneOffset.UTC),
        props);
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
}
