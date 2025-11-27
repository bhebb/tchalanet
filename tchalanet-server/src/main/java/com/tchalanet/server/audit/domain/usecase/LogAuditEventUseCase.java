package com.tchalanet.server.audit.domain.usecase;

import com.tchalanet.server.audit.domain.model.AuditEvent;

public interface LogAuditEventUseCase {
  void log(AuditEvent event);
}
