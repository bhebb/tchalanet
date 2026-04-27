package com.tchalanet.server.core.audit.application.query.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.audit.application.port.out.AuditEventReaderPort;
import com.tchalanet.server.core.audit.application.query.model.AuditEventQuery;
import com.tchalanet.server.core.audit.domain.model.AuditEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@UseCase
public class ListRecentAuditEventQueryHandler {
  private final AuditEventReaderPort reader;

  @Transactional(readOnly = true)
  public List<AuditEvent> handle(AuditEventQuery query) {
    return reader.findRecentForTenant(query.tenant(), query.limit());
  }
}
