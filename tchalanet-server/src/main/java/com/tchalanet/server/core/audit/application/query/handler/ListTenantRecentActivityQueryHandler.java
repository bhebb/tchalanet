package com.tchalanet.server.core.audit.application.query.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.audit.application.port.out.AuditEventReaderPort;
import com.tchalanet.server.core.audit.application.query.model.ActivityItemDto;
import com.tchalanet.server.core.audit.application.query.model.AuditEventQuery;
import com.tchalanet.server.core.audit.domain.model.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@UseCase
public class ListTenantRecentActivityQueryHandler {

  private final AuditEventReaderPort reader;

  @Transactional(readOnly = true)
  public List<ActivityItemDto> handle(AuditEventQuery query) {
    List<AuditEvent> events = reader.findRecentForTenant(query.tenant(), query.limit());

    return events.stream().map(ev -> new ActivityItemDto(
        ev.id(),
        ev.createdAt(),
        ev.entityType(),
        ev.entityId(),
        ev.action(),
        ev.actorType(),
        ev.actorId(),
        ev.action().name() + " "+ ev.entityType().name() + "/" + ev.entityId(),
        ev.detailsJson()
    )).collect(Collectors.toList());
  }
}
