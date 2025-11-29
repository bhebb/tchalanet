package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.RefreshPublicDrawsCacheCommand;

public interface RefreshPublicDrawsCacheCommandHandler {
  void handle(RefreshPublicDrawsCacheCommand command);
}
