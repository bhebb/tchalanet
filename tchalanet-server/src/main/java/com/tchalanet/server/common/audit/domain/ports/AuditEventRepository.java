package com.tchalanet.server.common.audit.domain.ports;

import com.tchalanet.server.common.audit.domain.model.AuditEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository {

  AuditEvent save(AuditEvent event);

  List<AuditEvent> findRecentForTenant(UUID tenantId, int limit);

  /** Delete audit events that occurred before the threshold. Returns number of rows deleted. */
  int deleteBefore(Instant threshold);
}
