package com.tchalanet.server.core.audit.application.port.in;

import com.tchalanet.server.core.audit.application.command.model.PurgeOldAuditEventsCommand;

public interface PurgeOldAuditEventsCommandHandler {
  void handle(PurgeOldAuditEventsCommand command);
}
