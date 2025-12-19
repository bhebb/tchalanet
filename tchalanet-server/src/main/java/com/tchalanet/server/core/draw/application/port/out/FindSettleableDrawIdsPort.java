package com.tchalanet.server.core.draw.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FindSettleableDrawIdsPort {
  List<UUID> findSettleableDrawIds(SettleableDrawCriteria criteria);

  record SettleableDrawCriteria(
      UUID tenantId,
      String source,
      String provider,
      String channelCode,
      Instant from,
      Instant to,
      Long maxDraws,
      boolean force) {}
}
