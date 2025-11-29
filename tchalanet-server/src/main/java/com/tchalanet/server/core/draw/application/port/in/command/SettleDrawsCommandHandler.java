package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.SettleDrawCommand;

public interface SettleDrawsCommandHandler {
  void handle(SettleDrawCommand command);
}
