package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.RefreshInternalDrawCacheCommand;

public interface RefreshInternalDrawCacheCommandHandler {
  void handle(RefreshInternalDrawCacheCommand command);
}
