package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.ReopenDrawForSalesCommand;

public interface ReopenDrawForSalesCommandHandler {
  void handle(ReopenDrawForSalesCommand command);
}
