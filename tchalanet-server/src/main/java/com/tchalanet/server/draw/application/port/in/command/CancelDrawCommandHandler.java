package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.CancelDrawCommand;

public interface CancelDrawCommandHandler {
  void handle(CancelDrawCommand command);
}
