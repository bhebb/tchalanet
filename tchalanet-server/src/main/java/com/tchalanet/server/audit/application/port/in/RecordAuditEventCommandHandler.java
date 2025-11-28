package com.tchalanet.server.audit.application.port.in;

import com.tchalanet.server.audit.application.command.model.RecordAuditEventCommand;

public interface RecordAuditEventCommandHandler {
  void handle(RecordAuditEventCommand command);
}
