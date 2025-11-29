package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.FetchAndApplyExternalResultCommand;

public interface FetchAndApplyExternalResultCommandHandler {
  void handle(FetchAndApplyExternalResultCommand command);
}
