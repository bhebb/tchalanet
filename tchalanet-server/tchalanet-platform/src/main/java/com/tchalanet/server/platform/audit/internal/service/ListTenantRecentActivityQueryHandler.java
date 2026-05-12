package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.platform.audit.api.model.ActivityItemDto;
import com.tchalanet.server.platform.audit.api.model.AuditEventQuery;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@UseCase
public class ListTenantRecentActivityQueryHandler {

  private final AuditEventReaderPort reader;

  @Transactional(readOnly = true)
  public List<ActivityItemDto> handle(AuditEventQuery query) {
    List<AuditEvent> events = reader.findRecentForTenant(query.tenant(), query.limit());

    return events.stream()
        .map(
            ev ->
                new ActivityItemDto(
                    ev.id(),
                    ev.occurredAt(),
                    ev.entityType(),
                    ev.entityId(),
                    ev.action(),
                    ev.actorType(),
                    ev.actorId().toString(),
                    ev.action().name() + " " + ev.entityType().name() + "/" + ev.entityId(),
                    ev.detailsJson()))
        .collect(Collectors.toList());
  }
}
