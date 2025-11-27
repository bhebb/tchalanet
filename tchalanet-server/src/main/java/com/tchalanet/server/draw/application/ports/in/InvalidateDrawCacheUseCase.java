package com.tchalanet.server.draw.application.ports.in;

import java.util.UUID;

public interface InvalidateDrawCacheUseCase {
  void invalidateTenant(UUID tenantId);
}
