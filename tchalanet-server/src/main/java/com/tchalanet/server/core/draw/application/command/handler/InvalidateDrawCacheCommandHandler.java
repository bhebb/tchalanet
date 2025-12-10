package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.InvalidateDrawCacheCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class InvalidateDrawCacheCommandHandler
    implements VoidCommandHandler<InvalidateDrawCacheCommand> {

  @Override
  public void handle(InvalidateDrawCacheCommand command) {
    log.warn(
        "InvalidateDrawCacheCommandHandler is a placeholder and does not implement actual invalidation logic.");
  }
}
