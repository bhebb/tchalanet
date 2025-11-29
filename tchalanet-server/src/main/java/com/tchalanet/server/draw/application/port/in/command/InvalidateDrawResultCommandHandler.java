package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.InvalidateDrawResultCommand;

public interface InvalidateDrawResultCommandHandler {
  void handle(InvalidateDrawResultCommand command);
}
