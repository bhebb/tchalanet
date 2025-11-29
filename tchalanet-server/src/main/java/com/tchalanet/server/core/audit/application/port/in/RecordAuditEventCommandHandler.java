package com.tchalanet.server.core.audit.application.port.in;

import com.tchalanet.server.core.audit.application.command.model.RecordAuditEventCommand;

public interface RecordAuditEventCommandHandler {
  void handle(RecordAuditEventCommand command);
}
