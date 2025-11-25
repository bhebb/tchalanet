package com.tchalanet.server.tenant.domain.usecase;

import com.tchalanet.server.tenant.domain.model.TenantGame;
import java.util.List;
import java.util.UUID;

public interface TenantGameCrudUseCase {
  TenantGame create(TenantGame t);

  List<TenantGame> listByTenant(UUID tenantId);
}
