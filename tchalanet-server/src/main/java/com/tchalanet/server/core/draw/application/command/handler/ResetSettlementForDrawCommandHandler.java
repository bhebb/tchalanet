package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.ResetSettlementForDrawCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ResetSettlementForDrawCommandHandler
    implements VoidCommandHandler<ResetSettlementForDrawCommand> {

  @Override
  public void handle(ResetSettlementForDrawCommand command) {
    log.info("ResetSettlementForDrawCommandHandler.handle - placeholder for command={}", command);
    // TODO: réinitialiser l'état de settlement du tirage et effectuer les opérations nécessaires
  }
}
