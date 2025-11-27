package com.tchalanet.server.draw.domain.ports.in;

import java.util.UUID;

public interface CloseDrawForSalesUseCase {
  void closeSales(UUID tenantId, UUID drawId);
}
