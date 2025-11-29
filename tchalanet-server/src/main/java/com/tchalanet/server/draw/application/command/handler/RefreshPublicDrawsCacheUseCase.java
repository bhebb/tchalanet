package com.tchalanet.server.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.application.command.model.RefreshPublicDrawsCacheCommand;
import com.tchalanet.server.draw.application.port.in.command.RefreshPublicDrawsCacheCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RefreshPublicDrawsCacheUseCase implements RefreshPublicDrawsCacheCommandHandler {

  @Override
  public void handle(RefreshPublicDrawsCacheCommand command) {
    log.warn(
        "RefreshPublicDrawCacheCommandHandler is a placeholder and does not implement actual cache refresh logic.");
  }
}
