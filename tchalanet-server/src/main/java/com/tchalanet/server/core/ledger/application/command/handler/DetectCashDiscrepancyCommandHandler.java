package com.tchalanet.server.core.ledger.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.command.model.DetectCashDiscrepancyCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class DetectCashDiscrepancyCommandHandler implements VoidCommandHandler<DetectCashDiscrepancyCommand> {

  @Override
  public void handle(DetectCashDiscrepancyCommand command) {
    // TODO: implement detection logic
    throw new UnsupportedOperationException("DetectCashDiscrepancyCommandHandler not implemented yet");
  }
}
