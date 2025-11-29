package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.CancelDrawCommand;

public interface CancelDrawCommandHandler {
  void handle(CancelDrawCommand command);
}
