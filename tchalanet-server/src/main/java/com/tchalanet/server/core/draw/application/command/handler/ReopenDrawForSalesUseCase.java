package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.ReopenDrawForSalesCommand;
import com.tchalanet.server.core.draw.application.port.in.command.ReopenDrawForSalesCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ReopenDrawForSalesUseCase implements ReopenDrawForSalesCommandHandler {

  @Override
  public void handle(ReopenDrawForSalesCommand command) {
    log.info("ReopenDrawForSalesUseCase.handle - placeholder for command={}", command);
    // TODO: rouvrir le tirage pour les ventes via les ports out
  }
}
