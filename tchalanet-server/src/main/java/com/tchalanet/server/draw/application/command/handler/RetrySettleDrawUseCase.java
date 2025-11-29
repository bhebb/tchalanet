package com.tchalanet.server.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.application.command.model.RetrySettleDrawCommand;
import com.tchalanet.server.draw.application.port.in.command.RetrySettleDrawCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RetrySettleDrawUseCase implements RetrySettleDrawCommandHandler {

  @Override
  public void handle(RetrySettleDrawCommand command) {
    log.info("RetrySettleDrawUseCase.handle - placeholder for command={}", command);
    // TODO: réessayer le règlement d'un draw via les ports out
  }
}
