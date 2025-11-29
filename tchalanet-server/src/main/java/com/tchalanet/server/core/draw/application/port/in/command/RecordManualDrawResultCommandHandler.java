package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.RecordManualDrawResultCommand;

public interface RecordManualDrawResultCommandHandler {
  void handle(RecordManualDrawResultCommand command);
}
