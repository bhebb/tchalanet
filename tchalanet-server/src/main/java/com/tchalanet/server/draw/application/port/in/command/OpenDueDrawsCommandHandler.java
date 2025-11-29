package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.OpenDueDrawsCommand;

public interface OpenDueDrawsCommandHandler {
  void handle(OpenDueDrawsCommand command);
}
