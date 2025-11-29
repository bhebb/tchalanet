package com.tchalanet.server.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.application.command.model.ResetSettlementForDrawCommand;
import com.tchalanet.server.draw.application.port.in.command.ResetSettlementForDrawCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ResetSettlementForDrawUseCase implements ResetSettlementForDrawCommandHandler {

  @Override
  public void handle(ResetSettlementForDrawCommand command) {
    log.info("ResetSettlementForDrawUseCase.handle - placeholder for command={}", command);
    // TODO: réinitialiser l'état de settlement du tirage et effectuer les opérations nécessaires
  }
}
