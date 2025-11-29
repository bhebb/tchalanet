package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.OverrideDrawResultCommand;

public interface OverrideDrawResultCommandHandler {
  void handle(OverrideDrawResultCommand command);
}
