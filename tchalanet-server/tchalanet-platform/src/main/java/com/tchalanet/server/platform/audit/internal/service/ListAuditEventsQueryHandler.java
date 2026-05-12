package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.platform.audit.api.model.ListAuditEventsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ListAuditEventsQueryHandler
    implements QueryHandler<ListAuditEventsQuery, TchPage<AuditEvent>> {

  private final AuditEventReaderPort reader;

  @Override
  @Transactional(readOnly = true)
  public TchPage<AuditEvent> handle(ListAuditEventsQuery query) {
    return reader.findByCriteria(new AuditEventsCriteria(
        query.tenantId(),
        query.entityType(),
        normalize(query.entityId()),
        query.action(),
        normalize(query.actorId()),
        query.from(),
        query.to(),
        query.pageable()));
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
