package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.RefreshInternalDrawCacheCommand;
import com.tchalanet.server.core.draw.application.port.in.command.RefreshInternalDrawCacheCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RefreshInternalDrawCacheUseCase implements RefreshInternalDrawCacheCommandHandler {

  @Override
  public void handle(RefreshInternalDrawCacheCommand command) {
    log.warn(
        "RefreshPublicDrawCacheCommandHandler is a placeholder and does not implement actual cache refresh logic.");
  }
}
