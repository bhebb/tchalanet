package com.tchalanet.server.tenant.domain.ports;

import com.tchalanet.server.tenant.domain.model.TenantUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantUserRepository {
  TenantUser save(TenantUser t);

  List<TenantUser> findByTenantId(UUID tenantId);

  List<TenantUser> findByUserId(UUID userId);

  Optional<TenantUser> findByTenantIdAndUserId(UUID tenantId, UUID userId);

  void deleteByTenantIdAndUserId(UUID tenantId, UUID userId);
}
