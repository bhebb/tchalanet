package com.tchalanet.server.core.ledger.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.command.model.ReverseTransactionCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class ReverseTransactionCommandHandler implements VoidCommandHandler<ReverseTransactionCommand> {

  @Override
  public void handle(ReverseTransactionCommand command) {
    // TODO: implement reversal with audit trail
    throw new UnsupportedOperationException("ReverseTransactionCommandHandler not implemented yet");
  }
}

