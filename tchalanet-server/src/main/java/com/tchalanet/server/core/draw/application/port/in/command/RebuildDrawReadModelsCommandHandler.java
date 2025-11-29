package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.RebuildDrawReadModelsCommand;

public interface RebuildDrawReadModelsCommandHandler {
  void handle(RebuildDrawReadModelsCommand command);
}
