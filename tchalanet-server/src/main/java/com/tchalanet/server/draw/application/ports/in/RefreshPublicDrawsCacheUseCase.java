package com.tchalanet.server.draw.application.ports.in;

import java.util.UUID;

public interface RefreshPublicDrawsCacheUseCase {
  void refreshCache(UUID tenantId);
}
