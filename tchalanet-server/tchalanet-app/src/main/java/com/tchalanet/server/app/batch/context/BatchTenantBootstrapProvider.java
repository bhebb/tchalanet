package com.tchalanet.server.app.batch.context;

import com.tchalanet.server.common.types.id.TenantId;
import java.util.Optional;

public interface BatchTenantBootstrapProvider {
  Optional<BatchTenantBootstrap> findBootstrapById(TenantId tenantId);
}
