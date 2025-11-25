package com.tchalanet.server.common.audit.domain.usecase;

import com.tchalanet.server.common.audit.domain.model.AuditAction;
import com.tchalanet.server.common.audit.domain.model.AuditEntityType;
import com.tchalanet.server.common.audit.domain.model.AuditEvent;
import com.tchalanet.server.common.audit.domain.ports.AuditEventRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogAuditEventUseCase {

  private final AuditEventRepository repository;
  private final AuditEventFactory factory;

  public AuditEvent log(
      AuditEntityType entityType,
      String entityId,
      AuditAction action,
      Map<String, Object> details) {
    AuditEvent event = factory.build(entityType, entityId, action, details);
    return repository.save(event);
  }
}
