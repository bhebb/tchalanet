package com.tchalanet.server.core.draw.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DrawBatchQueryPort {

  List<ClosedDrawRef> findClosedDrawsForDate(
      com.tchalanet.server.common.types.id.TenantId tenantIdOrNull,
      LocalDate drawDate,
      List<String> channelCodes,
      int maxDraws);

  record ClosedDrawRef(UUID drawId, String channelCode) {}
}
