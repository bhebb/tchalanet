package com.tchalanet.server.common.audit.adapters;

import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.common.audit.domain.usecase.LogAuditEventUseCase;
import org.springframework.stereotype.Component;

@Component
public class AuditLoggingBridge implements LogAuditEventUseCase {

  private final com.tchalanet.server.audit.domain.usecase.LogAuditEventUseCase delegate;

  public AuditLoggingBridge(
      com.tchalanet.server.audit.domain.usecase.LogAuditEventUseCase delegate) {
    this.delegate = delegate;
  }

  @Override
  public void log(AuditEvent event) {
    delegate.log(event);
  }
}
