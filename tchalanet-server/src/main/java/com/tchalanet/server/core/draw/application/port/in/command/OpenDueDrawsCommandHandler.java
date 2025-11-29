package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsCommand;

public interface OpenDueDrawsCommandHandler {
  void handle(OpenDueDrawsCommand command);
}
