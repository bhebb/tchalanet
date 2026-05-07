package com.tchalanet.server.core.audit.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.audit.domain.model.AuditEvent;
import java.util.List;

public interface AuditEventReaderPort {
  List<AuditEvent> findRecentForTenant(TenantId tenantId, int limit);

  TchPage<AuditEvent> findByCriteria(AuditEventsCriteria criteria);
}
