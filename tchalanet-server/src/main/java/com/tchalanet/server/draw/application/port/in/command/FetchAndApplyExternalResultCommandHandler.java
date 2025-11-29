package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.FetchAndApplyExternalResultCommand;

public interface FetchAndApplyExternalResultCommandHandler {
  void handle(FetchAndApplyExternalResultCommand command);
}
