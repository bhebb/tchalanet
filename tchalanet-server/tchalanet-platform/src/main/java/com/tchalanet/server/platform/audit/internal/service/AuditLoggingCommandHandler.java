package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.platform.audit.api.model.request.LogAuditEventRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingCommandHandler {

  private final AuditEventWriterPort writer;
  private final AuditEventFactory factory;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(LogAuditEventRequest command) {
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
