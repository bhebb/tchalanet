package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.InvalidateDrawResultCommand;

public interface InvalidateDrawResultCommandHandler {
  void handle(InvalidateDrawResultCommand command);
}
