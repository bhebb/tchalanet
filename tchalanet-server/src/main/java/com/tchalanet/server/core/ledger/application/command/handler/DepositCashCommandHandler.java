package com.tchalanet.server.core.ledger.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.command.model.DepositCashCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class DepositCashCommandHandler implements VoidCommandHandler<DepositCashCommand> {

  @Override
  public void handle(DepositCashCommand command) {
    // TODO: implement deposit logic
    throw new UnsupportedOperationException("DepositCashCommandHandler not implemented yet");
  }
}

