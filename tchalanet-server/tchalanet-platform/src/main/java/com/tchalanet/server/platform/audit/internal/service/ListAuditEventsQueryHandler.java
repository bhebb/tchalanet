package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.platform.audit.api.model.AuditEventView;
import com.tchalanet.server.platform.audit.api.model.request.ListAuditEventsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ListAuditEventsQueryHandler {

  private final AuditEventReaderPort reader;

  @Transactional(readOnly = true)
  public TchPage<AuditEventView> handle(ListAuditEventsRequest query) {
    var page = reader.findByCriteria(new AuditEventsCriteria(
        query.tenantId(),
        query.entityType(),
        normalize(query.entityId()),
        query.action(),
        normalize(query.actorId()),
        query.from(),
        query.to(),
        query.pageable()));
    return TchPageMapper.map(page, AuditEventMapper::toView);
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
