package com.tchalanet.server.core.tenant.application.ports.in;

import com.tchalanet.server.core.tenant.domain.model.Tenant;
import java.util.Optional;

/**
 * Inbound Port for resolving a Tenant domain object based on a host identifier. This is typically
 * used by web filters or central components to establish the TenantContext.
 */
public interface ResolveTenantByHostUseCase {
  Optional<Tenant> resolveTenant(String host);
}
