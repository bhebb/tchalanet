package com.tchalanet.server.core.draw.domain.model;

import java.util.Map;
import java.util.Set;

public class DrawStatusTransition {

  private static final Map<DrawStatus, Set<DrawStatus>> ALLOWED =
      Map.of(
          DrawStatus.SCHEDULED, Set.of(DrawStatus.OPEN, DrawStatus.CANCELED),
          DrawStatus.OPEN, Set.of(DrawStatus.CLOSED, DrawStatus.CANCELED),
          DrawStatus.CLOSED, Set.of(DrawStatus.RESULTED, DrawStatus.CANCELED),
          DrawStatus.RESULTED, Set.of(DrawStatus.SETTLED, DrawStatus.CANCELED),
          DrawStatus.SETTLED, Set.of(DrawStatus.RESULTED),
          DrawStatus.CANCELED, Set.of(DrawStatus.SCHEDULED, DrawStatus.OPEN));

  private DrawStatusTransition() {}

  public static void check(DrawStatus from, DrawStatus to) {
    if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
      throw new IllegalStateException("Invalid transition: " + from + " -> " + to);
    }
  }
}
