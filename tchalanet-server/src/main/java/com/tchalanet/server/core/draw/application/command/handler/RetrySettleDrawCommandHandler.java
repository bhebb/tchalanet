package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.RetrySettleDrawCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RetrySettleDrawCommandHandler implements VoidCommandHandler<RetrySettleDrawCommand> {

  @Override
  public void handle(RetrySettleDrawCommand command) {
    log.info("RetrySettleDrawCommandHandler.handle - placeholder for command={}", command);
    // TODO: réessayer le règlement d'un draw via les ports out
  }
}
