package com.tchalanet.server.audit.application.query.handler;

import com.tchalanet.server.audit.application.port.out.AuditEventReaderPort;
import com.tchalanet.server.audit.application.query.model.AuditEventQuery;
import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.common.stereotype.UseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@UseCase
public class ListRecentAuditEventUseCase {
  private final AuditEventReaderPort reader;

  @Transactional(readOnly = true)
  public List<AuditEvent> handle(AuditEventQuery query) {
    return reader.findRecentForTenant(query.tenant(), query.limit());
  }
}
