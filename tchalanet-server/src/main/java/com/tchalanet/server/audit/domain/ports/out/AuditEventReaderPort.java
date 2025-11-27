package com.tchalanet.server.audit.domain.ports.out;

import com.tchalanet.server.audit.domain.model.AuditEvent;
import java.util.List;
import java.util.UUID;

public interface AuditEventReaderPort {
  List<AuditEvent> findRecentForTenant(UUID tenantId, int limit);
}
