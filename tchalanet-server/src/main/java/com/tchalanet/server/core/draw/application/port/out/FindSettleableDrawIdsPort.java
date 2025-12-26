package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.List;

public interface FindSettleableDrawIdsPort {
  List<DrawId> findSettleableDrawIds(SettleableDrawCriteria criteria);

  record SettleableDrawCriteria(
      TenantId tenantId,
      String source,
      String provider,
      String channelCode,
      Instant from,
      Instant to,
      Long maxDraws,
      boolean force) {}
}
