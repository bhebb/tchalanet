package com.tchalanet.server.draw.domain.usecase;

import java.util.UUID;

public interface InvalidateDrawCacheUseCase {
  void invalidateTenant(UUID tenantId);
}
