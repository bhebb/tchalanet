package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.command.model.ReprintTicketCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Handler to trigger ticket reprint (side-effect: printing / logging) */
@UseCase
@RequiredArgsConstructor
@Component
public class ReprintTicketCommandHandler implements VoidCommandHandler<ReprintTicketCommand> {

  // Inject printers / event publishers here

  @Override
  public void handle(ReprintTicketCommand command) {
    // TODO: implement reprint logic
    throw new UnsupportedOperationException("ReprintTicketCommandHandler not implemented yet");
  }
}

