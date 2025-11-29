package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.RefreshInternalDrawCacheCommand;

public interface RefreshInternalDrawCacheCommandHandler {
  void handle(RefreshInternalDrawCacheCommand command);
}
