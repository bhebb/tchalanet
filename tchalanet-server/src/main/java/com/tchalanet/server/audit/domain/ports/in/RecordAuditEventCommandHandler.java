package com.tchalanet.server.audit.domain.ports.in;

import com.tchalanet.server.audit.application.command.model.RecordAuditEventCommand;

public interface RecordAuditEventCommandHandler {
  void handle(RecordAuditEventCommand command);
}
