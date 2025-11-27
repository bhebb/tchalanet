package com.tchalanet.server.tenant.application.ports.in;

import com.tchalanet.server.tenant.domain.model.Tenant;
import java.util.Optional;

public interface ResolveTenantUseCase {
  Optional<Tenant> resolveTenant(String host);
}
