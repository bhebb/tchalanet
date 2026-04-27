package com.tchalanet.server.core.sales.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Minimal read model needed to compute winnings. */
public interface DrawResultViewPort {
  /**
   * Minimal view for ticket winning calculation.
   * Input is global drawResultId (canonical).
   */
  DrawResultMinimalView findById(UUID drawResultId);

  record DrawResultMinimalView(
      UUID id,
      String slotKey,
      Instant occurredAt,
      String lot1,
      String lot2,
      String lot3,
      String pick3,
      List<String> twoDigits) {}
}
