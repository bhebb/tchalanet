package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.RefreshPublicDrawsCacheCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RefreshPublicDrawsCacheCommandHandler
    implements VoidCommandHandler<RefreshPublicDrawsCacheCommand> {

  @Override
  public void handle(RefreshPublicDrawsCacheCommand command) {
    log.warn(
        "RefreshPublicDrawCacheCommandHandler is a placeholder and does not implement actual cache refresh logic.");
  }
}
