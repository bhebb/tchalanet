package com.tchalanet.server.core.tenant.application.ports.in;

import com.tchalanet.server.core.tenant.domain.model.Tenant;
import java.util.Optional;

public interface ResolveTenantUseCase {
  Optional<Tenant> resolveTenant(String host);
}
