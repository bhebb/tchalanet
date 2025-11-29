package com.tchalanet.server.core.audit.application.port.in;

import com.tchalanet.server.core.audit.application.query.model.AuditEventQuery;
import com.tchalanet.server.core.audit.domain.model.AuditEvent;
import java.util.List;

public interface ListRecentAuditEventsQueryHandler {
  List<AuditEvent> findAuditLogs(AuditEventQuery query);
}
