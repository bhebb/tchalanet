package com.tchalanet.server.core.tenant.domain.ports;

import com.tchalanet.server.core.tenant.domain.model.TenantGame;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.util.List;

public interface TenantGameRepository {
  TenantGame save(TenantGame t);

  List<TenantGame> findByTenant(TenantId tenantId);

  void deleteById(java.util.UUID id);
}
