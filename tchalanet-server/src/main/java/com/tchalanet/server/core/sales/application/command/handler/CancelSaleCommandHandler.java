package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.command.model.CancelSaleCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Handler to cancel a sale (void a ticket). */
@UseCase
@RequiredArgsConstructor
@Component
public class CancelSaleCommandHandler implements VoidCommandHandler<CancelSaleCommand> {

  // Inject necessary ports here

  @Override
  public void handle(CancelSaleCommand command) {
    // TODO: implement cancel sale logic
    throw new UnsupportedOperationException("CancelSaleCommandHandler not implemented yet");
  }
}

