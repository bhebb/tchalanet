package com.tchalanet.server.stats.domain.ports.in;

import java.util.UUID;

/** Inbound Port for triggering the statistical calculation for a single POS session. */
public interface ComputeSessionStatsUseCase {
  void computeAndSaveStatsForSession(UUID sessionId);
}
