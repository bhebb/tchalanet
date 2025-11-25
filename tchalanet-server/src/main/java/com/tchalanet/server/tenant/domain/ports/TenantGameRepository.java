package com.tchalanet.server.tenant.domain.ports;

import com.tchalanet.server.common.domain.TenantId;
import com.tchalanet.server.tenant.domain.model.TenantGame;
import java.util.List;

public interface TenantGameRepository {
  TenantGame save(TenantGame t);

  //  Optional<TenantGame> findByTenantAndGameCode(TenantId tenantId, String gameCode);
  List<TenantGame> findByTenant(TenantId tenantId);

  void deleteById(java.util.UUID id);
}
