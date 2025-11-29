package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.OverrideDrawResultCommand;

public interface OverrideDrawResultCommandHandler {
  void handle(OverrideDrawResultCommand command);
}
