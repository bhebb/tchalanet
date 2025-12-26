package com.tchalanet.server.core.tenant.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;

public interface TenantOutletCheckPort {
  boolean hasActiveOutlets(TenantId tenantId);
}
