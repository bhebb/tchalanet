package com.tchalanet.server.core.draw.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FindFetchableDrawIdsPort {
  List<UUID> findFetchableDrawIds(Instant since);

  /**
   * Variante utilisée par les déclenchements OPS, avec critères supplémentaires.
   */
  default List<UUID> findFetchableDrawIdsForOps(
      UUID tenantId,
      String source,
      String provider,
      String channelCode,
      int daysBack,
      int maxDraws,
      Instant now) {
    // Impl par défaut : fallback sur le comportement historique.
    return findFetchableDrawIds(now.minusSeconds(daysBack * 24L * 3600L));
  }
}
