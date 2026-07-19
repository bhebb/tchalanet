package com.tchalanet.server.core.haiti.internal.infra.adapter;

import com.tchalanet.server.core.haiti.api.HaitiFlags;
import com.tchalanet.server.core.haiti.api.HaitiProjectionOutput;
import com.tchalanet.server.core.haiti.internal.application.port.out.HaitiLotteryPort;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.ExternalPick;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.HaitiProjectionConfig;
import com.tchalanet.server.core.haiti.internal.domain.lottery.service.HaitiResultProjector;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Adapter that exposes the Haiti projector as a port for application usage. */
@Component
@Slf4j
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
      log.error("haiti.projection failed", e);
      var flags = HaitiFlags.fail(PROJECTION_VERSION, RULE_SET, "PROJECTION_FAILED", now, Map.of());
      return HaitiProjectionOutput.fail(flags);
    }
  }
}
