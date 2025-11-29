package com.tchalanet.server.core.audit.application.port.in;

import com.tchalanet.server.core.audit.application.command.model.LogAuditEventCommand;

public interface LogAuditEventCommandHandler {
  void handle(LogAuditEventCommand command);
}
