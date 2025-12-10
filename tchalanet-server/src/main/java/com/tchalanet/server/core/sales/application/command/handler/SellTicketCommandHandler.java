package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.app.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.command.model.SellTicketCommand;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Handler for selling tickets. Returns created ticket id. */
@UseCase
@RequiredArgsConstructor
@Component
public class SellTicketCommandHandler implements CommandHandler<SellTicketCommand, UUID> {

  // Inject necessary ports here (repositories, event publishers, etc.)

  @Override
  public UUID handle(SellTicketCommand command) {
    // TODO: implement selling logic (validation, limits, persistence, events)
    throw new UnsupportedOperationException("SellTicketCommandHandler not implemented yet");
  }
}

