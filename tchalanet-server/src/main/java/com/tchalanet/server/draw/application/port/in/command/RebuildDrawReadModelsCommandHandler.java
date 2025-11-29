package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.RebuildDrawReadModelsCommand;

public interface RebuildDrawReadModelsCommandHandler {
  void handle(RebuildDrawReadModelsCommand command);
}
