package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.SettleDrawCommand;

public interface SettleDrawsCommandHandler {
  void handle(SettleDrawCommand command);
}
