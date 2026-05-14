package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.platform.audit.api.model.request.AuditEventRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@UseCase
public class ListRecentAuditEventQueryHandler {
  private final AuditEventReaderPort reader;

  @Transactional(readOnly = true)
  public List<AuditEvent> handle(AuditEventRequest query) {
    return reader.findRecentForTenant(query.tenant(), query.limit());
  }
}
