package com.tchalanet.server.stats.domain.ports.in;

import java.util.UUID;

/** Inbound Port for triggering the statistical calculation for a single draw. */
public interface ComputeDrawStatsUseCase {
  void computeAndSaveStatsForDraw(UUID drawId);
}
