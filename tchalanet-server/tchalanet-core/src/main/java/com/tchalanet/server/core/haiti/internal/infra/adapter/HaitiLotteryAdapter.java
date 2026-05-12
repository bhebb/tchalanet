package com.tchalanet.server.core.haiti.internal.infra.adapter;

import com.tchalanet.server.common.contracts.haiti.HaitiFlags;
import com.tchalanet.server.common.contracts.haiti.HaitiProjectionOutput;
import com.tchalanet.server.core.haiti.internal.application.port.out.HaitiLotteryPort;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.ExternalPick;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.HaitiProjectionConfig;
import com.tchalanet.server.core.haiti.internal.domain.lottery.service.HaitiResultProjector;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Adapter that exposes the Haiti projector as a port for application usage. */
@Component
public class HaitiLotteryAdapter implements HaitiLotteryPort {

  // bump when you change projection semantics
  private static final int PROJECTION_VERSION = 1;
  private static final String RULE_SET = "DEFAULT";

  private final HaitiResultProjector projector;
  private final Clock clock;

  public HaitiLotteryAdapter(HaitiResultProjector projector, Clock clock) {
    this.projector = projector;
    this.clock = clock;
  }

  @Override
  public HaitiProjectionOutput projectResult(
      ExternalPick externalPick, HaitiProjectionConfig config) {
    final Instant now = Instant.now(clock);

    try {
      var result = projector.project(config, externalPick);
      var flags = new HaitiFlags(PROJECTION_VERSION, true, "", RULE_SET, now, Map.of());
      return HaitiProjectionOutput.ok(result, flags);

    } catch (Exception e) {
      var reason =
          (e.getMessage() == null || e.getMessage().isBlank())
              ? "PROJECTION_FAILED"
              : e.getMessage();

      var flags =
          HaitiFlags.fail(
              PROJECTION_VERSION,
              RULE_SET,
              reason,
              now,
              Map.of("error", e.getClass().getSimpleName()));
      return HaitiProjectionOutput.fail(flags);
    }
  }
}
