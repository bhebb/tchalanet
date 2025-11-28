package com.tchalanet.server.audit.application.port.in;

import com.tchalanet.server.audit.application.command.model.LogAuditEventCommand;

public interface LogAuditEventCommandHandler {
  void handle(LogAuditEventCommand command);
}
