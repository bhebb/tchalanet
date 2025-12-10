package com.tchalanet.server.core.tenant.infra.adapter;

import com.tchalanet.server.core.tenant.application.port.out.TenantRepositoryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StubTenantRepositoryAdapter implements TenantRepositoryPort {

  @Override
  public boolean hasActiveOutlets(UUID tenantId) {
    // stub: assume no active outlets
    return false;
  }

  @Override
  public void archiveTenant(UUID tenantId, String reason) {
    // stub: noop
  }
}

