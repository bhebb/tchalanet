package com.tchalanet.server.core.ledger.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.command.model.WithdrawCashCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class WithdrawCashCommandHandler implements VoidCommandHandler<WithdrawCashCommand> {

  @Override
  public void handle(WithdrawCashCommand command) {
    // TODO: implement withdrawal logic
    throw new UnsupportedOperationException("WithdrawCashCommandHandler not implemented yet");
  }
}

