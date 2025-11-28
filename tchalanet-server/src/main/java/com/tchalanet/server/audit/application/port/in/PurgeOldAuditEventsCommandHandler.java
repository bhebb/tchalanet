package com.tchalanet.server.audit.application.port.in;

import com.tchalanet.server.audit.application.command.model.PurgeOldAuditEventsCommand;

public interface PurgeOldAuditEventsCommandHandler {
  void handle(PurgeOldAuditEventsCommand command);
}
