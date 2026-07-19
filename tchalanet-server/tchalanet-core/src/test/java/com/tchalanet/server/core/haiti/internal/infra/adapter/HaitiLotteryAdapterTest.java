package com.tchalanet.server.core.haiti.internal.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.haiti.internal.domain.lottery.model.ExternalPick;
import com.tchalanet.server.core.haiti.internal.domain.lottery.service.HaitiResultProjector;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class HaitiLotteryAdapterTest {

  @Test
  void projectionFailureNeverExposesProjectorDiagnosticsInFlags() {
    HaitiResultProjector projector =
        (config, pick) -> {
          throw new IllegalStateException("missing projection token LOT2");
        };
    var adapter =
        new HaitiLotteryAdapter(
            projector, Clock.fixed(Instant.parse("2026-07-18T12:00:00Z"), ZoneOffset.UTC));

    var output = adapter.projectResult(ExternalPick.of("123", "4567"), null);

    assertThat(output.result()).isNull();
    assertThat(output.flags().projectionOk()).isFalse();
    assertThat(output.flags().projectionReason()).isEqualTo("PROJECTION_FAILED");
    assertThat(output.flags().extra()).isEmpty();
  }
}
