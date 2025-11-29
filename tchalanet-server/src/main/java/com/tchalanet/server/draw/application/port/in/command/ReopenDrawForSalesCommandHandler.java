package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.ReopenDrawForSalesCommand;

public interface ReopenDrawForSalesCommandHandler {
  void handle(ReopenDrawForSalesCommand command);
}
