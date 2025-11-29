package com.tchalanet.server.core.audit.application.port.out;

import com.tchalanet.server.core.audit.domain.model.AuditEvent;
import java.util.List;
import java.util.UUID;

public interface AuditEventReaderPort {
  List<AuditEvent> findRecentForTenant(UUID tenantId, int limit);
}
