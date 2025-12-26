package com.tchalanet.server.core.audit.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.audit.application.command.model.LogAuditEventCommand;
import com.tchalanet.server.core.audit.application.port.out.AuditEventWriterPort;
import com.tchalanet.server.core.audit.domain.service.AuditEventFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingCommandHandler implements VoidCommandHandler<LogAuditEventCommand> {

  private final AuditEventWriterPort writer;
  private final AuditEventFactory factory;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(LogAuditEventCommand command) {
    if (command == null) {
      return;
    }
    var event =
        factory.build(
            command.entityType(), command.entityId(), command.action(), command.details());
    if (event == null) {
      return;
    }
    try {
      writer.save(event);
    } catch (Exception e) {
      log.error("Failed to write audit event", e);
    }
  }
}
