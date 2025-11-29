package com.tchalanet.server.core.draw.application.port.in.command;

import java.util.UUID;

public interface InvalidateDrawCacheCommandHandler {
  void invalidateTenant(UUID tenantId);
}
