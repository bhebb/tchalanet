package com.tchalanet.server.features.private_dashboard.block;

import java.time.Instant;
import java.util.List;

public record ActivityFeedBlock(List<ActivityItem> items) {
  public static ActivityFeedBlock empty() {
    return new ActivityFeedBlock(List.of());
  }

  public record ActivityItem(
      String id,
      String actorId,
      String actionKey,
      String summary,
      String entityType,
      String entityId,
      Instant occurredAt) {}
}
