package com.tchalanet.server.audit.application.port.in;

import com.tchalanet.server.audit.application.query.model.AuditEventQuery;
import com.tchalanet.server.audit.domain.model.AuditEvent;
import java.util.List;

public interface ListRecentAuditEventsQueryHandler {
  List<AuditEvent> findAuditLogs(AuditEventQuery query);
}
