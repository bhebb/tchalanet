package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import java.util.List;

public interface AuditEventReaderPort {
  List<AuditEvent> findRecentForTenant(TenantId tenantId, int limit);

  TchPage<AuditEvent> findByCriteria(AuditEventsCriteria criteria);
}
