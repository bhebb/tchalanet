package com.tchalanet.server.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.application.port.in.command.InvalidateDrawCacheCommandHandler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class InvalidateDrawCacheUseCase implements InvalidateDrawCacheCommandHandler {

  @Override
  public void invalidateTenant(UUID tenantId) {
    log.warn(
        "InvalidateDrawCacheCommandHandler is a placeholder and does not implement actual invalidation logic.");
  }
}
