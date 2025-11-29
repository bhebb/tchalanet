package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.RefreshPublicDrawsCacheCommand;

public interface RefreshPublicDrawsCacheCommandHandler {
  void handle(RefreshPublicDrawsCacheCommand command);
}
