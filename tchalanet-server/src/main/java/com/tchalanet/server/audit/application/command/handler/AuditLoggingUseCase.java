package com.tchalanet.server.audit.application.command.handler;

import com.tchalanet.server.audit.application.command.model.LogAuditEventCommand;
import com.tchalanet.server.audit.application.port.in.LogAuditEventCommandHandler;
import com.tchalanet.server.audit.application.port.out.AuditEventWriterPort;
import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.audit.domain.service.AuditEventFactory;
import com.tchalanet.server.common.domain.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingUseCase implements LogAuditEventCommandHandler {

  private final AuditEventWriterPort writer;
  private final AuditEventFactory factory;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(LogAuditEventCommand command) {
    if (command == null) return;
    AuditEvent event =
        factory.build(
            command.entityType(), command.entityId(), command.action(), command.details());
    if (event == null) return;
    try {
      writer.save(event);
    } catch (Exception e) {
      log.error("Failed to write audit event", e);
    }
  }
}
